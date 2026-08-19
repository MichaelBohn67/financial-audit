package de.bohnottensen.financialaudit.application.usecase.analytics;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.AmlEngine;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingAnalysisServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private AmlEngine amlEngine;

    @Mock
    private AuditTrailWriter auditTrailWriter;

    private BookingAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new BookingAnalysisService(bookingRepository, findingRepository, amlEngine, auditTrailWriter);
    }

    @Test
    void shouldRunAnalysisAndCreateHighAndMediumSeverityFindings() {
        Booking booking1 = new Booking();
        booking1.setId(1L);
        booking1.setDescription("Large transfer");
        booking1.setAmount(new BigDecimal("15000.00"));
        booking1.setCurrency("EUR");
        booking1.setTransactionTimestamp(LocalDateTime.now());

        Booking booking2 = new Booking();
        booking2.setId(2L);
        booking2.setDescription("Medium suspicious transfer");
        booking2.setAmount(new BigDecimal("8000.00"));
        booking2.setCurrency("EUR");
        booking2.setTransactionTimestamp(LocalDateTime.now());

        when(bookingRepository.findAll()).thenReturn(List.of(booking1, booking2));
        when(amlEngine.check(booking1)).thenReturn(List.of("High amount exceeded threshold"));
        when(amlEngine.check(booking2)).thenReturn(List.of("Unusual pattern alert"));

        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> {
            Finding f = invocation.getArgument(0);
            f.setId(100L);
            return f;
        });

        AnalysisRunRequest request = new AnalysisRunRequest("v2.1", "compliance-check-q3");
        List<Finding> findings = service.run(request);

        assertThat(findings).hasSize(2);
        assertThat(findings.get(0).getRiskLevel()).isEqualTo("HIGH");
        assertThat(findings.get(0).getRuleVersion()).isEqualTo("v2.1");
        assertThat(findings.get(0).getRunContext()).isEqualTo("compliance-check-q3");
        assertThat(findings.get(0).getStatus()).isEqualTo("NEW");
        assertThat(findings.get(0).getRuleName()).isEqualTo("AML_RULE");

        assertThat(findings.get(1).getRiskLevel()).isEqualTo("MEDIUM");

        verify(auditTrailWriter, times(2)).record(
                eq("FINDING"),
                eq(100L),
                eq("FINDING_CREATED"),
                eq("SYSTEM_AML"),
                eq("Finding created by AML analysis"),
                isNull(),
                anyString()
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoBookingsExist() {
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        List<Finding> findings = service.run(new AnalysisRunRequest("v1", "test"));

        assertThat(findings).isEmpty();
        verifyNoInteractions(amlEngine);
        verifyNoInteractions(findingRepository);
        verifyNoInteractions(auditTrailWriter);
    }

    @Test
    void shouldHandleNullBookingOnFindingSnapshot() {
        Booking booking = new Booking();
        booking.setId(3L);

        when(bookingRepository.findAll()).thenReturn(List.of(booking));
        when(amlEngine.check(booking)).thenReturn(List.of("General alert"));

        when(findingRepository.save(any(Finding.class))).thenAnswer(invocation -> {
            Finding f = invocation.getArgument(0);
            f.setId(200L);
            return f;
        });

        List<Finding> findings = service.run(new AnalysisRunRequest("v1", "test"));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRiskLevel()).isEqualTo("MEDIUM");
    }
}
