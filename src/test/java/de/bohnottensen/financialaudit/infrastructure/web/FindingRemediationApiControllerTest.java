package de.bohnottensen.financialaudit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bohnottensen.financialaudit.application.usecase.finding.FindingManagementService;
import de.bohnottensen.financialaudit.domain.model.Finding;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FindingRemediationApiControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private FindingManagementService service;

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void auditorCanUpdateRemediationPlan() throws Exception {
        when(service.updatePlan(anyLong(), anyString(), anyString())).thenReturn(new Finding());

        mockMvc.perform(patch("/api/findings/1/remediation/plan")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("plan", "Implement control"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void auditorCanAssignRemediation() throws Exception {
        when(service.assign(anyLong(), anyString(), any(LocalDate.class), anyString()))
                .thenReturn(new Finding());

        mockMvc.perform(post("/api/findings/1/remediation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "owner", "owner", "dueDate", "2026-09-01"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void auditorCanLinkWorkpaper() throws Exception {
        when(service.linkWorkpaper(anyLong(), anyLong(), anyString())).thenReturn(new Finding());

        mockMvc.perform(post("/api/findings/1/workpaper/5")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void auditorCanTransitionRemediationStatus() throws Exception {
        when(service.transition(anyLong(), anyString(), anyString(), anyString())).thenReturn(new Finding());

        mockMvc.perform(patch("/api/findings/1/remediation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "RESOLVED", "comment", "Issue fixed"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "USER")
    void nonAuditRoleCannotUpdateRemediation() throws Exception {
        mockMvc.perform(patch("/api/findings/1/remediation/plan")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("plan", "Attempt"))))
                .andExpect(status().isForbidden());
    }
}
