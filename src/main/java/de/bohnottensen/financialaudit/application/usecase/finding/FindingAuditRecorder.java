package de.bohnottensen.financialaudit.application.usecase.finding;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import org.springframework.stereotype.Component;

/**
 * Single place that persists a newly detected finding together with its audit trail entry.
 *
 * Five analysis services previously each carried their own copy of the same
 * save-then-record sequence and of the identical {@code findingSnapshot} builder, which
 * meant any change to the snapshot format had to be made in five places.
 */
@Component
public class FindingAuditRecorder {

    /** Status assigned to every finding at creation time. */
    public static final String INITIAL_STATUS = "NEW";

    private final FindingRepository findingRepository;
    private final AuditTrailWriter auditTrailWriter;

    public FindingAuditRecorder(FindingRepository findingRepository, AuditTrailWriter auditTrailWriter) {
        this.findingRepository = findingRepository;
        this.auditTrailWriter = auditTrailWriter;
    }

    /**
     * Persist the finding and record a FINDING_CREATED audit event for it.
     *
     * @param finding      fully populated finding; its status defaults to {@link #INITIAL_STATUS}
     * @param actorSource  short marker naming the producing subsystem, e.g. SYSTEM_AML
     * @param summary      human readable summary of why the finding was created
     * @return the persisted finding with its generated id
     */
    public Finding record(Finding finding, String actorSource, String summary) {
        if (finding.getStatus() == null || finding.getStatus().isBlank()) {
            finding.setStatus(INITIAL_STATUS);
        }
        Finding saved = findingRepository.save(finding);
        auditTrailWriter.record(
                "FINDING",
                saved.getId(),
                "FINDING_CREATED",
                actorSource,
                summary,
                null,
                snapshot(saved)
        );
        return saved;
    }

    /**
     * Stable, human readable representation of a finding used as the audit trail payload.
     */
    public String snapshot(Finding finding) {
        return "bookingId=" + (finding.getBooking() != null ? finding.getBooking().getId() : null)
                + ";ruleName=" + finding.getRuleName()
                + ";riskLevel=" + finding.getRiskLevel()
                + ";status=" + finding.getStatus()
                + ";analysisRunId=" + finding.getAnalysisRunId()
                + ";ruleVersion=" + finding.getRuleVersion()
                + ";runContext=" + finding.getRunContext();
    }
}
