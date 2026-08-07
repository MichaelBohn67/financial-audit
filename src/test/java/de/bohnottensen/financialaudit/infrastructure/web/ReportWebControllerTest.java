package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.reporting.ReportContent;
import de.bohnottensen.financialaudit.application.usecase.reporting.ReportExportService;
import de.bohnottensen.financialaudit.application.usecase.reporting.ReportService;
import de.bohnottensen.financialaudit.domain.model.ReportRun;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReportWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportExportService reportExportService;

    @MockBean
    private ReportService reportService;

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void shouldViewReport() throws Exception {
        ReportRun run = new ReportRun();
        run.setReportName("AML-Report");
        when(reportService.findRunById(1L)).thenReturn(run);

        ReportContent content = new ReportContent(
                "AML-Report", "1.0.0", null, "senior", null,
                new ReportContent.FindingsSummary(0, 0, 0, 0, 0, 0, List.of()),
                new ReportContent.BookingStats(0, BigDecimal.ZERO, 0),
                List.of()
        );
        when(reportExportService.assemble(1L)).thenReturn(content);

        mockMvc.perform(get("/reports/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("report-view"))
                .andExpect(model().attributeExists("report"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AML-Report")));
    }

    @Test
    @WithMockUser(username = "auditor", roles = "AUDITOR")
    void shouldViewManagementLetter() throws Exception {
        ReportRun run = new ReportRun();
        run.setReportName("Management Letter 2026");
        when(reportService.findRunById(2L)).thenReturn(run);

        ReportContent content = new ReportContent(
                "Management Letter 2026", "1.0.0", null, "lead-auditor", null,
                new ReportContent.FindingsSummary(1, 1, 0, 0, 1, 0, List.of(
                        new ReportContent.FindingEntry(1L, 101L, "AML-Rule", "Suspicious activity", "HIGH", "NEW", "RUN-1", "1.0", null)
                )),
                new ReportContent.BookingStats(100, new BigDecimal("50000.00"), 1),
                List.of()
        );
        when(reportExportService.assemble(2L)).thenReturn(content);

        mockMvc.perform(get("/reports/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("management-letter"))
                .andExpect(model().attributeExists("report"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Management Letter 2026")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Audit & Assurance Services")));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void user_shouldNotViewReport() throws Exception {
        mockMvc.perform(get("/reports/1"))
                .andExpect(status().isForbidden());
    }
}
