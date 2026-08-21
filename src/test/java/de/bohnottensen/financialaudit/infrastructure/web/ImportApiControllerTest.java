package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.importing.ImportJobResult;
import de.bohnottensen.financialaudit.application.usecase.importing.ImportOrchestratorService;
import de.bohnottensen.financialaudit.application.usecase.importing.ImportRunContext;
import de.bohnottensen.financialaudit.application.usecase.importing.ImportValidationError;
import de.bohnottensen.financialaudit.infrastructure.security.ScopeAccessPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImportApiController.class)
class ImportApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportOrchestratorService importOrchestratorService;

    @MockBean
    private ScopeAccessPolicy scopeAccessPolicy;

    @Test
    @WithMockUser(roles = "ASSISTANT")
    void shouldImportCsvUsingOrchestrator() throws Exception {
        when(scopeAccessPolicy.canAccessDocument(any(), any(), any(), any())).thenReturn(true);
        when(importOrchestratorService.importFrom(any(ImportRunContext.class), any())).thenReturn(sampleResult("CSV"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bookings.csv",
                "text/csv",
                ("description,amount,currency,transactionTimestamp,sourceAccount,destinationAccount\n" +
                        "Invoice,100.00,EUR,2026-07-20T10:00:00,SRC,DST").getBytes()
        );

        mockMvc.perform(multipart("/api/imports/csv")
                        .file(file)
                        .with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1")
                        .param("documentId", "DOC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("CSV"))
                .andExpect(jsonPath("$.importedCount").value(2))
                .andExpect(jsonPath("$.invalidCount").value(1));
    }

    @Test
    @WithMockUser(roles = "ASSISTANT")
    void shouldRejectEmptyCsv() throws Exception {
        when(scopeAccessPolicy.canAccessDocument(any(), any(), any(), any())).thenReturn(true);
        MockMultipartFile empty = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> mockMvc.perform(multipart("/api/imports/csv")
                        .file(empty)
                        .with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1")
                        .param("documentId", "DOC-1")).andReturn())
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSV file must not be empty");
    }

    @Test
    @WithMockUser(roles = "SENIOR_AUDITOR")
    void shouldImportOpenBankingUsingOrchestrator() throws Exception {
        when(scopeAccessPolicy.canAccessDocument(any(), any(), any(), any())).thenReturn(true);
        when(importOrchestratorService.importFrom(any(ImportRunContext.class), any())).thenReturn(sampleResult("OPEN_BANKING"));

        mockMvc.perform(post("/api/imports/open-banking")
                        .with(csrf())
                        .param("tenantId", "TENANT-1")
                        .param("projectId", "PROJECT-1")
                        .param("documentId", "DOC-2")
                        .param("accountId", "DE123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("OPEN_BANKING"))
                .andExpect(jsonPath("$.checksum").value("abc123"));
    }

    private ImportJobResult sampleResult(String sourceType) {
        LocalDateTime started = LocalDateTime.parse("2026-08-03T17:00:00");
        LocalDateTime finished = LocalDateTime.parse("2026-08-03T17:00:05");
        return new ImportJobResult(
                42L,
                "COMPLETED",
                "tenantId=TENANT-1;projectId=PROJECT-1;documentId=DOC-1;label=test",
                sourceType,
                started,
                finished,
                2,
                1,
                List.of(new ImportValidationError(1, "example")),
                "abc123"
        );
    }
}
