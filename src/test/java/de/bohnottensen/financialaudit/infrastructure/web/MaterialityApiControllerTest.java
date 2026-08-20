package de.bohnottensen.financialaudit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bohnottensen.financialaudit.application.usecase.materiality.MaterialityService;
import de.bohnottensen.financialaudit.domain.model.MaterialityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MaterialityApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MaterialityService materialityService;

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void shouldReturnActiveMaterialityConfig() throws Exception {
        MaterialityConfig config = new MaterialityConfig();
        config.setId(1L);
        config.setName("Default-2026");
        config.setPlanningMateriality(new BigDecimal("100000.00"));
        config.setPerformanceMateriality(new BigDecimal("50000.00"));
        config.setDeMinimisThreshold(new BigDecimal("5000.00"));
        config.setActive(true);

        when(materialityService.active()).thenReturn(config);

        mockMvc.perform(get("/api/materiality"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Default-2026"))
                .andExpect(jsonPath("$.planningMateriality").value(100000.00))
                .andExpect(jsonPath("$.performanceMateriality").value(50000.00))
                .andExpect(jsonPath("$.deMinimisThreshold").value(5000.00))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(username = "lead", roles = "LEAD_AUDITOR")
    void shouldAllowLeadAuditorToSaveConfig() throws Exception {
        MaterialityConfig config = new MaterialityConfig();
        config.setId(2L);
        config.setName("Custom-2026");
        config.setPlanningMateriality(new BigDecimal("200000.00"));
        config.setPerformanceMateriality(new BigDecimal("100000.00"));
        config.setDeMinimisThreshold(new BigDecimal("10000.00"));
        config.setActive(true);

        when(materialityService.save(eq("Custom-2026"), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(config);

        MaterialityApiController.Request request = new MaterialityApiController.Request(
                "Custom-2026",
                new BigDecimal("200000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("10000.00")
        );

        mockMvc.perform(post("/api/materiality")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Custom-2026"));
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void shouldDenyAuditorFromSavingConfig() throws Exception {
        MaterialityApiController.Request request = new MaterialityApiController.Request(
                "Denied-2026",
                new BigDecimal("200000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("10000.00")
        );

        mockMvc.perform(post("/api/materiality")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void shouldEvaluateAccount() throws Exception {
        MaterialityService.MaterialityResult res = new MaterialityService.MaterialityResult(
                10L, new BigDecimal("1000.00"), "OVERALL", true, 99L);
        when(materialityService.evaluateAll("ACC-100")).thenReturn(List.of(res));

        mockMvc.perform(get("/api/materiality/accounts/ACC-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(10))
                .andExpect(jsonPath("$[0].classification").value("OVERALL"))
                .andExpect(jsonPath("$[0].material").value(true))
                .andExpect(jsonPath("$[0].findingId").value(99));
    }
}
