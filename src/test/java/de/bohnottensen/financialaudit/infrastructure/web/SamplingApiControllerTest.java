package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.sampling.SamplingService;
import de.bohnottensen.financialaudit.domain.model.SamplingRun;
import de.bohnottensen.financialaudit.domain.model.SamplingRunItem;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunItemRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class SamplingApiControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean SamplingService samplingService;
    @MockBean SamplingRunRepository samplingRuns;
    @MockBean SamplingRunItemRepository samplingItems;

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void authorizedUserCanCreateMusRun() throws Exception {
        SamplingRun run = run(10L);
        when(samplingService.generateMusSample("Q1", 100, 5, 42)).thenReturn(run);

        mockMvc.perform(post("/api/sampling/mus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("runName", "Q1", "populationSize", 100, "sampleSize", 5, "seed", 42))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.samplingStrategy").value("MUS"));
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void invalidMusRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/sampling/mus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("runName", "Q1", "populationSize", 100, "sampleSize", 0, "seed", 42))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void nonAuditRoleCannotUseSamplingApi() throws Exception {
        mockMvc.perform(get("/api/sampling/runs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void canReadRunItems() throws Exception {
        when(samplingRuns.existsById(10L)).thenReturn(true);
        when(samplingItems.findBySamplingRunIdOrderBySampleUnitIndex(10L)).thenReturn(List.of(new SamplingRunItem()));

        mockMvc.perform(get("/api/sampling/runs/10/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void canListRuns() throws Exception {
        when(samplingRuns.findAll()).thenReturn(List.of(run(10L)));

        mockMvc.perform(get("/api/sampling/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void readingItemsForNonExistentRunReturnsNotFound() throws Exception {
        when(samplingRuns.existsById(999L)).thenReturn(false);

        mockMvc.perform(get("/api/sampling/runs/999/items"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void exceptionHandlerReturnsBadRequestOnIllegalArgumentException() throws Exception {
        when(samplingService.generateMusSample("Q1", 100, 5, 42))
                .thenThrow(new IllegalArgumentException("Sample size exceeds population"));

        mockMvc.perform(post("/api/sampling/mus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("runName", "Q1", "populationSize", 100, "sampleSize", 5, "seed", 42))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sample size exceeds population"));
    }

    @Test
    void unauthenticatedUserCannotUseSamplingApi() throws Exception {
        mockMvc.perform(get("/api/sampling/runs")).andExpect(status().isUnauthorized());
    }

    private SamplingRun run(Long id) {
        SamplingRun run = new SamplingRun();
        run.setId(id);
        run.setRunName("Q1");
        run.setSamplingStrategy("MUS");
        run.setPopulationSize(100L);
        run.setSampleSize(5L);
        return run;
    }
}
