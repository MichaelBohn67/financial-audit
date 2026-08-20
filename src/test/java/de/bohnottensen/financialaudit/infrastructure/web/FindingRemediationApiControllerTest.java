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
        Finding finding = new Finding();
        finding.setId(1L);
        finding.setRemediationPlan("Implement control");
        when(service.updatePlan(anyLong(), anyString(), anyString())).thenReturn(finding);

        mockMvc.perform(patch("/api/findings/1/remediation/plan")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("plan", "Implement control"))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.remediationPlan").value("Implement control"));
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void auditorCanAssignRemediation() throws Exception {
        Finding finding = new Finding();
        finding.setId(1L);
        finding.setRemediationOwner("owner");
        when(service.assign(anyLong(), anyString(), any(LocalDate.class), anyString()))
                .thenReturn(finding);

        mockMvc.perform(post("/api/findings/1/remediation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "owner", "owner", "dueDate", "2026-09-01"))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.remediationOwner").value("owner"));
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void auditorCanLinkWorkpaper() throws Exception {
        Finding finding = new Finding();
        finding.setId(1L);
        when(service.linkWorkpaper(anyLong(), anyLong(), anyString())).thenReturn(finding);

        mockMvc.perform(post("/api/findings/1/workpaper/5")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void auditorCanTransitionRemediationStatus() throws Exception {
        Finding finding = new Finding();
        finding.setId(1L);
        finding.setRemediationStatus("RESOLVED");
        finding.setResolutionComment("Issue fixed");
        when(service.transition(anyLong(), anyString(), anyString(), anyString())).thenReturn(finding);

        mockMvc.perform(patch("/api/findings/1/remediation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "RESOLVED", "comment", "Issue fixed"))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.remediationStatus").value("RESOLVED"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.resolutionComment").value("Issue fixed"));
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
