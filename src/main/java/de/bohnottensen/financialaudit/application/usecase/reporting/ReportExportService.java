package de.bohnottensen.financialaudit.application.usecase.reporting;

import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.domain.model.ReportRun;
import de.bohnottensen.financialaudit.domain.model.SamplingRun;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunRepository;
import de.bohnottensen.financialaudit.domain.model.ReportArchive;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReportArchiveRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.HexFormat;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

/**
 * Assembles structured {@link ReportContent} from live audit artefacts and
 * serializes it as a JSON export artefact.
 * All logic is deterministic: same data always produces the same output.
 */
@Service
public class ReportExportService {

    private final FindingRepository findingRepository;
    private final BookingRepository bookingRepository;
    private final SamplingRunRepository samplingRunRepository;
    private final ReportService reportService;
    private final ReportArchiveRepository archiveRepository;
    private final ObjectMapper objectMapper;
    private final Path archiveDirectory;

    @Autowired
    public ReportExportService(FindingRepository findingRepository,
                               BookingRepository bookingRepository,
                               SamplingRunRepository samplingRunRepository,
                               ReportService reportService,
                               ReportArchiveRepository archiveRepository,
                               @Value("${financial-audit.reporting.archive-directory:./var/report-archive}") String archiveDirectory) {
        this.findingRepository = findingRepository;
        this.bookingRepository = bookingRepository;
        this.samplingRunRepository = samplingRunRepository;
        this.reportService = reportService;
        this.archiveRepository = archiveRepository;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.archiveDirectory = Path.of(archiveDirectory).toAbsolutePath().normalize();
    }

    public ReportExportService(FindingRepository findingRepository, BookingRepository bookingRepository,
                               SamplingRunRepository samplingRunRepository, ReportService reportService) {
        this(findingRepository, bookingRepository, samplingRunRepository, reportService, null, "./var/report-archive");
    }

    /**
     * Assemble full report content for a given run.
     * The run must exist; its header fields are used as the report metadata.
     */
    public ReportContent assemble(Long runId) {
        ReportRun run = reportService.findRunById(runId);

        List<Finding> findings = findingRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<SamplingRun> samplingRuns = samplingRunRepository.findAll();

        return new ReportContent(
                run.getReportName(),
                run.getTemplateVersion(),
                run.getGeneratedAt(),
                run.getTriggeredBy(),
                run.getParameters(),
                buildFindingsSummary(findings),
                buildBookingStats(bookings, findings),
                buildSamplingRunSummaries(samplingRuns)
        );
    }

    public ReportContent assemble(Long runId, String tenantId, String projectId) {
        ReportRun run = reportService.findRunById(runId, tenantId, projectId);
        List<Finding> findings = findingRepository.findByBooking_TenantIdAndBooking_ProjectId(tenantId, projectId);
        List<Booking> bookings = bookingRepository.findByTenantIdAndProjectId(tenantId, projectId);
        List<SamplingRun> samplingRuns = samplingRunRepository.findByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId);
        return new ReportContent(run.getReportName(), run.getTemplateVersion(), run.getGeneratedAt(), run.getTriggeredBy(),
                run.getParameters(), buildFindingsSummary(findings), buildBookingStats(bookings, findings),
                buildSamplingRunSummaries(samplingRuns));
    }

    public ExportArtifact archive(Long runId, String tenantId, String projectId, String archivedBy) {
        if (archiveRepository == null) throw new IllegalStateException("Report archiving is not available");
        if (archivedBy == null || archivedBy.isBlank()) throw new IllegalArgumentException("Archive actor is required");
        ReportRun run = reportService.findRunById(runId, tenantId, projectId);
        if (!de.bohnottensen.financialaudit.domain.model.ReportRunStatus.COMPLETED.name().equals(run.getStatus())) {
            throw new IllegalStateException("Only completed reports can be archived");
        }
        ReportArchive existing = archiveRepository.findByReportRunIdAndTenantIdAndProjectId(runId, tenantId, projectId).orElse(null);
        if (existing != null) return new ExportArtifact(existing.getId(), existing.getStoragePath(), existing.getSha256(), existing.getContentLength(), existing.getArchivedAt());
        try {
            ReportContent content = assemble(runId, tenantId, projectId);
            byte[] bytes = objectMapper.writeValueAsBytes(content);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            Files.createDirectories(archiveDirectory);
            Path target = archiveDirectory.resolve("report-" + runId + "-" + tenantId + "-" + projectId + ".json").normalize();
            if (!target.startsWith(archiveDirectory)) throw new IllegalArgumentException("Invalid archive path");
            Path temporary = Files.createTempFile(archiveDirectory, "report-" + runId + "-", ".tmp");
            try { Files.write(temporary, bytes); Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            finally { Files.deleteIfExists(temporary); }
            ReportArchive archive = new ReportArchive(); archive.setReportRunId(runId); archive.setTenantId(tenantId); archive.setProjectId(projectId);
            archive.setStoragePath(target.toString()); archive.setSha256(hash); archive.setContentLength((long) bytes.length); archive.setArchivedBy(archivedBy);
            archive.setManifest("reportRunId=" + runId + ";tenantId=" + tenantId + ";projectId=" + projectId + ";sha256=" + hash + ";length=" + bytes.length);
            ReportArchive saved = archiveRepository.save(archive);
            return new ExportArtifact(saved.getId(), saved.getStoragePath(), saved.getSha256(), saved.getContentLength(), saved.getArchivedAt());
        } catch (Exception e) { throw new IllegalStateException("Unable to archive report", e); }
    }

    public record ExportArtifact(Long archiveId, String storagePath, String sha256, Long contentLength,
                                 java.time.LocalDateTime archivedAt) {}

    private ReportContent.FindingsSummary buildFindingsSummary(List<Finding> findings) {
        long total = findings.size();
        long high = findings.stream().filter(f -> "HIGH".equalsIgnoreCase(f.getRiskLevel())).count();
        long medium = findings.stream().filter(f -> "MEDIUM".equalsIgnoreCase(f.getRiskLevel())).count();
        long low = findings.stream().filter(f -> "LOW".equalsIgnoreCase(f.getRiskLevel())).count();
        long newCount = findings.stream().filter(f -> "NEW".equalsIgnoreCase(f.getStatus())).count();
        long escalated = findings.stream().filter(f -> "ESCALATED".equalsIgnoreCase(f.getStatus())).count();

        List<ReportContent.FindingEntry> entries = findings.stream()
                .map(f -> new ReportContent.FindingEntry(
                        f.getId(),
                        f.getBooking() != null ? f.getBooking().getId() : null,
                        f.getRuleName(),
                        f.getAlertDescription(),
                        f.getRiskLevel(),
                        f.getStatus(),
                        f.getAnalysisRunId(),
                        f.getRuleVersion(),
                        f.getCreatedAt()
                ))
                .toList();

        return new ReportContent.FindingsSummary(total, high, medium, low, newCount, escalated, entries);
    }

    private ReportContent.BookingStats buildBookingStats(List<Booking> bookings, List<Finding> findings) {
        long total = bookings.size();
        BigDecimal totalAmount = bookings.stream()
                .map(Booking::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<Long> bookingIdsWithFindings = findings.stream()
                .filter(f -> f.getBooking() != null)
                .map(f -> f.getBooking().getId())
                .collect(Collectors.toSet());
        long withFindings = bookingIdsWithFindings.size();

        return new ReportContent.BookingStats(total, totalAmount, withFindings);
    }

    private List<ReportContent.SamplingRunSummary> buildSamplingRunSummaries(List<SamplingRun> samplingRuns) {
        return samplingRuns.stream()
                .map(s -> new ReportContent.SamplingRunSummary(
                        s.getId(),
                        s.getRunName(),
                        s.getSamplingStrategy(),
                        s.getSeed(),
                        s.getPopulationSize(),
                        s.getSampleSize(),
                        s.getCreatedAt()
                ))
                .toList();
    }
}
