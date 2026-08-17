package de.bohnottensen.financialaudit.application.usecase.materiality;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.domain.model.MaterialityConfig;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.MaterialityConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MaterialityService {
    private static final String MATERIALITY_RULE = "MATERIALITY_THRESHOLD";

    private final MaterialityConfigRepository configs;
    private final BookingRepository bookings;
    private final FindingRepository findings;
    private final AuditTrailWriter audit;

    public MaterialityService(MaterialityConfigRepository configs, BookingRepository bookings,
                               FindingRepository findings, AuditTrailWriter audit) {
        this.configs = configs;
        this.bookings = bookings;
        this.findings = findings;
        this.audit = audit;
    }

    @Transactional
    public MaterialityConfig save(String name, BigDecimal overall, BigDecimal performance, BigDecimal deMinimis) {
        if (name == null || name.isBlank() || overall == null || performance == null || deMinimis == null
                || overall.signum() <= 0 || performance.signum() <= 0 || deMinimis.signum() < 0
                || performance.compareTo(overall) > 0 || deMinimis.compareTo(performance) > 0) {
            throw new IllegalArgumentException(
                    "Materiality thresholds must satisfy 0 <= de minimis <= performance <= overall");
        }

        configs.findAll().forEach(config -> {
            config.setActive(false);
            configs.save(config);
        });

        MaterialityConfig config = new MaterialityConfig();
        config.setName(name);
        config.setPlanningMateriality(overall);
        config.setPerformanceMateriality(performance);
        config.setDeMinimisThreshold(deMinimis);
        config.setActive(true);
        MaterialityConfig saved = configs.save(config);
        audit.record("MaterialityConfig", saved.getId(), "CREATE", "system",
                "Materiality configured", null, name);
        return saved;
    }

    public MaterialityConfig active() {
        return configs.findFirstByActiveTrueOrderByCreatedAtDesc()
                .orElseThrow(() -> new IllegalStateException("No active materiality configuration"));
    }

    public MaterialityResult evaluate(Booking booking) {
        return classify(booking, active());
    }

    @Transactional
    public List<MaterialityResult> evaluateAll(String account) {
        MaterialityConfig config = active();
        return bookings.findBySourceAccountOrDestinationAccount(account, account).stream()
                .map(booking -> evaluateAndPersist(booking, account, config))
                .toList();
    }

    private MaterialityResult evaluateAndPersist(Booking booking, String scope, MaterialityConfig config) {
        MaterialityResult result = classify(booking, config);
        if (!result.material()) {
            return result;
        }

        Finding finding = findings.findFirstByBookingIdAndMaterialityConfigIdAndRuleName(
                        booking.getId(), config.getId(), MATERIALITY_RULE)
                .orElseGet(() -> createMaterialityFinding(booking, scope, config, result));
        return result.withFindingId(finding.getId());
    }

    private MaterialityResult classify(Booking booking, MaterialityConfig config) {
        if (booking == null || booking.getAmount() == null) {
            throw new IllegalArgumentException("Booking and booking amount are required");
        }

        BigDecimal amount = booking.getAmount().abs();
        String classification = amount.compareTo(config.getPlanningMateriality()) >= 0 ? "OVERALL" :
                amount.compareTo(config.getPerformanceMateriality()) >= 0 ? "PERFORMANCE" :
                amount.compareTo(config.getDeMinimisThreshold()) >= 0 ? "DE_MINIMIS" : "BELOW_THRESHOLD";
        return new MaterialityResult(booking.getId(), amount, classification,
                !classification.equals("BELOW_THRESHOLD"), null);
    }

    private Finding createMaterialityFinding(Booking booking, String scope,
                                             MaterialityConfig config, MaterialityResult result) {
        Finding finding = new Finding();
        finding.setBooking(booking);
        finding.setMaterialityConfig(config);
        finding.setMaterialityClassification(result.classification());
        finding.setRuleName(MATERIALITY_RULE);
        finding.setAlertDescription("Booking amount meets the " + result.classification()
                + " materiality threshold");
        finding.setRiskLevel(result.classification().equals("OVERALL") ? "HIGH" :
                result.classification().equals("PERFORMANCE") ? "MEDIUM" : "LOW");
        finding.setStatus("NEW");
        finding.setAnalysisRunId("MAT-" + config.getId());
        finding.setRuleVersion("1");
        finding.setRunContext(scope == null ? "ALL" : scope);

        Finding saved = findings.save(finding);
        audit.record("FINDING", saved.getId(), "FINDING_CREATED", "SYSTEM_MATERIALITY",
                "Finding created by materiality evaluation", null, findingSnapshot(saved));
        return saved;
    }

    private String findingSnapshot(Finding finding) {
        return "bookingId=" + finding.getBooking().getId()
                + ";materialityConfigId=" + finding.getMaterialityConfig().getId()
                + ";classification=" + finding.getMaterialityClassification()
                + ";riskLevel=" + finding.getRiskLevel()
                + ";status=" + finding.getStatus();
    }

    public record MaterialityResult(Long bookingId, BigDecimal amount, String classification,
                                    boolean material, Long findingId) {
        private MaterialityResult withFindingId(Long id) {
            return new MaterialityResult(bookingId, amount, classification, material, id);
        }
    }
}
