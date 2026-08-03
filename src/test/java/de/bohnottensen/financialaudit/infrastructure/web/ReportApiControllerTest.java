package de.bohnottensen.financialaudit.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bohnottensen.financialaudit.application.usecase.reporting.ReportContent;
import de.bohnottensen.financialaudit.application.usecase.reporting.ReportExportService;
import de.bohnottensen.financialaudit.application.usecase.reporting.ReportService;
import de.bohnottensen.financialaudit.domain.model.ReportRun;
import de.bohnottensen.financialaudit.domain.model.ReportRunStatus;
import de.bohnottensen.financialaudit.domain.model.ReportTemplate;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    @MockBean
    private ReportExportService reportExportService;

    // --- registerTemplate (WIRTSCHAFTSPRUEFER only) ---

    @Test
    @WithMockUser(username = "wirtschaftspruefer", roles = "WIRTSCHAFTSPRUEFER")
    void wirtschaftspruefer_shouldRegisterTemplate() throws Exception {
        when(reportService.registerTemplate(anyString(), anyString(), anyString()))
                .thenReturn(template(1L, "AML-Report", "1.0.0", true));

        mockMvc.perform(post("/api/reports/templates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "AML-Report", "version", "1.0.0", "description", "AML findings"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("AML-Report"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(username = "assistant", roles = "ASSISTANT")
    void assistant_shouldNotRegisterTemplate() throws Exception {
        mockMvc.perform(post("/api/reports/templates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "X", "version", "1.0", "description", "d"))))
                .andExpect(status().isForbidden());
    }

    // --- templateVersions ---

    @Test
    @WithMockUser(username = "senior", roles = "SENIOR_AUDITOR")
    void shouldListTemplateVersions() throws Exception {
        when(reportService.findTemplateVersions("AML-Report"))
                .thenReturn(List.of(
                        template(2L, "AML-Report", "2.0.0", true),
                        template(1L, "AML-Report", "1.0.0", false)));

        mockMvc.perform(get("/api/reports/templates/AML-Report/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value("2.0.0"))
                .andExpect(jsonPath("$[1].version").value("1.0.0"));
    }

    // --- activeTemplate ---

    @Test
    @WithMockUser(username = "senior", roles = "SENIOR_AUDITOR")
    void shouldGetActiveTemplate() throws Exception {
        when(reportService.findActiveTemplate("AML-Report"))
                .thenReturn(template(2L, "AML-Report", "2.0.0", true));

        mockMvc.perform(get("/api/reports/templates/AML-Report/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2.0.0"))
                .andExpect(jsonPath("$.active").value(true));
    }

    // --- startRun ---

    @Test
    @WithMockUser(username = "senior", roles = "SENIOR_AUDITOR")
    void seniorAuditor_shouldStartRun() throws Exception {
        ReportRun run = reportRun(10L, "AML-Report", "1.0.0", ReportRunStatus.RUNNING);
        ReportRun completed = reportRun(10L, "AML-Report", "1.0.0", ReportRunStatus.COMPLETED);
        when(reportService.startRun(anyString(), anyString(), anyString())).thenReturn(run);
        when(reportExportService.assemble(10L)).thenReturn(emptyContent());
        when(reportService.completeRun(anyLong(), anyString())).thenReturn(completed);

        mockMvc.perform(post("/api/reports/runs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("templateName", "AML-Report", "parameters", "tenantId=T1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "assistant", roles = "ASSISTANT")
    void assistant_shouldNotStartRun() throws Exception {
        mockMvc.perform(post("/api/reports/runs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("templateName", "AML-Report", "parameters", ""))))
                .andExpect(status().isForbidden());
    }

    // --- getRun ---

    @Test
    @WithMockUser(username = "assistant", roles = "ASSISTANT")
    void assistant_shouldGetRunById() throws Exception {
        when(reportService.findRunById(5L))
                .thenReturn(reportRun(5L, "AML-Report", "1.0.0", ReportRunStatus.COMPLETED));

        mockMvc.perform(get("/api/reports/runs/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // --- listRuns ---

    @Test
    @WithMockUser(username = "senior", roles = "SENIOR_AUDITOR")
    void shouldListCompletedRuns() throws Exception {
        when(reportService.findRunsByStatus(ReportRunStatus.COMPLETED))
                .thenReturn(List.of(reportRun(1L, "AML-Report", "1.0.0", ReportRunStatus.COMPLETED)));

        mockMvc.perform(get("/api/reports/runs").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    // --- exportRun ---

    @Test
    @WithMockUser(username = "senior", roles = "SENIOR_AUDITOR")
    void shouldExportReportContent() throws Exception {
        when(reportExportService.assemble(3L)).thenReturn(emptyContent());

        mockMvc.perform(get("/api/reports/runs/3/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportName").value("AML-Report"))
                .andExpect(jsonPath("$.findingsSummary.totalFindings").value(0));
    }

    @Test
    void unauthenticated_shouldBeDenied() throws Exception {
        mockMvc.perform(get("/api/reports/runs/1"))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private ReportTemplate template(Long id, String name, String version, boolean active) {
        ReportTemplate t = new ReportTemplate();
        t.setId(id);
        t.setName(name);
        t.setVersion(version);
        t.setActive(active);
        return t;
    }

    private ReportRun reportRun(Long id, String name, String version, ReportRunStatus status) {
        ReportRun r = new ReportRun();
        r.setId(id);
        r.setReportName(name);
        r.setTemplateVersion(version);
        r.setStatus(status.name());
        return r;
    }

    private ReportContent emptyContent() {
        return new ReportContent(
                "AML-Report", "1.0.0", null, "senior", null,
                new ReportContent.FindingsSummary(0, 0, 0, 0, 0, 0, List.of()),
                new ReportContent.BookingStats(0, BigDecimal.ZERO, 0),
                List.of()
        );
    }
}
