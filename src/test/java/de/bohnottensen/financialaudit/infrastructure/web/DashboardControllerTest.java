package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.dashboard.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean DashboardService dashboardService;

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void authorizedUserCanReadMetrics() throws Exception {
        when(dashboardService.metrics()).thenReturn(metrics());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(12))
                .andExpect(jsonPath("$.findingsByRisk.HIGH").value(2))
                .andExpect(jsonPath("$.auditProgress.completionPercentage").value(50.0))
                .andExpect(jsonPath("$.latestReports[0].name").value("Quarterly report"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void nonAuditRoleCannotReadMetricsOrPage() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(get("/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotReadDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "lead", roles = "LEAD_AUDITOR")
    void authorizedUserCanViewDashboardPage() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Findings by risk")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Recent audit events")));
    }

    private DashboardService.Metrics metrics() {
        LinkedHashMap<String, Long> risks = new LinkedHashMap<>();
        risks.put("HIGH", 2L);
        return new DashboardService.Metrics(12, 4, risks, risks, risks, 2, 1,
                risks, 3, new DashboardService.AuditProgress(4, 2, BigDecimal.valueOf(50.0)),
                List.of(new DashboardService.LatestReport(7L, "Quarterly report", "COMPLETED", null)), List.of());
    }
}
