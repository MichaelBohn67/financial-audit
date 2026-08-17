package de.bohnottensen.financialaudit.application.usecase.finding;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import de.bohnottensen.financialaudit.infrastructure.persistence.FindingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class FindingManagementService {
    private static final Set<String> STATUSES = Set.of(
            "OPEN", "IN_PROGRESS", "READY_FOR_REVIEW", "RESOLVED", "REJECTED", "CLOSED");

    private final FindingRepository findings;
    private final WorkpaperRepository workpapers;
    private final AuditTrailWriter audit;

    public FindingManagementService(FindingRepository findings, WorkpaperRepository workpapers,
                                    AuditTrailWriter audit) {
        this.findings = findings;
        this.workpapers = workpapers;
        this.audit = audit;
    }

    public Finding get(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Finding id is required");
        }
        return findings.findById(id).orElseThrow();
    }

    @Transactional
    public Finding linkWorkpaper(Long id, Long workpaperId, String actor) {
        requireActor(actor);
        if (workpaperId == null) {
            throw new IllegalArgumentException("Workpaper id is required");
        }
        Finding finding = get(id);
        ensureMutable(finding);
        Workpaper workpaper = workpapers.findById(workpaperId).orElseThrow();
        String previous = snapshot(finding);
        finding.setWorkpaper(workpaper);
        return change(finding, actor, "LINK_WORKPAPER", previous);
    }

    @Transactional
    public Finding assign(Long id, String owner, LocalDate dueDate, String actor) {
        requireActor(actor);
        if (owner == null || owner.isBlank() || dueDate == null) {
            throw new IllegalArgumentException("Remediation owner and due date are required");
        }
        Finding finding = get(id);
        ensureMutable(finding);
        String previous = snapshot(finding);
        finding.setRemediationOwner(owner.trim());
        finding.setRemediationDueDate(dueDate);
        return change(finding, actor, "ASSIGN_REMEDIATION", previous);
    }

    @Transactional
    public Finding updatePlan(Long id, String plan, String actor) {
        requireActor(actor);
        if (plan == null || plan.isBlank()) {
            throw new IllegalArgumentException("Remediation plan is required");
        }
        Finding finding = get(id);
        ensureMutable(finding);
        String previous = snapshot(finding);
        finding.setRemediationPlan(plan.trim());
        return change(finding, actor, "UPDATE_REMEDIATION_PLAN", previous);
    }

    @Transactional
    public Finding transition(Long id, String target, String comment, String actor) {
        requireActor(actor);
        String normalizedTarget = target == null ? null : target.trim().toUpperCase();
        Finding finding = get(id);
        String from = finding.getRemediationStatus();
        if (!allowed(from, normalizedTarget)) {
            throw new IllegalStateException("Invalid remediation transition: " + from + " -> " + target);
        }
        validateTargetRequirements(finding, normalizedTarget, comment);

        String previous = snapshot(finding);
        finding.setRemediationStatus(normalizedTarget);
        if (comment != null && !comment.isBlank()) {
            finding.setResolutionComment(comment.trim());
        }
        if ("RESOLVED".equals(normalizedTarget)) {
            finding.setResolvedAt(LocalDateTime.now());
            finding.setResolvedBy(actor);
        } else if ("REJECTED".equals(normalizedTarget)) {
            finding.setResolvedAt(null);
            finding.setResolvedBy(null);
        }
        return change(finding, actor, "REMEDIATION_" + normalizedTarget, previous);
    }

    private void validateTargetRequirements(Finding finding, String target, String comment) {
        if (Set.of("IN_PROGRESS", "READY_FOR_REVIEW", "RESOLVED").contains(target)) {
            if (finding.getRemediationOwner() == null || finding.getRemediationOwner().isBlank()
                    || finding.getRemediationDueDate() == null) {
                throw new IllegalStateException("Owner and due date are required before " + target);
            }
        }
        if (Set.of("READY_FOR_REVIEW", "RESOLVED").contains(target)
                && (finding.getRemediationPlan() == null || finding.getRemediationPlan().isBlank())) {
            throw new IllegalStateException("Remediation plan is required before " + target);
        }
        if (Set.of("RESOLVED", "REJECTED").contains(target)
                && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("A resolution comment is required for " + target);
        }
    }

    private Finding change(Finding finding, String actor, String event, String previous) {
        Finding saved = findings.save(finding);
        audit.record("FINDING", saved.getId(), event, actor,
                "Finding remediation changed", previous, snapshot(saved));
        return saved;
    }

    private boolean allowed(String from, String to) {
        if (from == null || to == null || !STATUSES.contains(to)) {
            return false;
        }
        return switch (from) {
            case "OPEN" -> to.equals("IN_PROGRESS");
            case "IN_PROGRESS" -> to.equals("READY_FOR_REVIEW");
            case "READY_FOR_REVIEW" -> to.equals("RESOLVED") || to.equals("REJECTED");
            case "REJECTED" -> to.equals("IN_PROGRESS");
            case "RESOLVED" -> to.equals("CLOSED");
            case "CLOSED" -> false;
            default -> false;
        };
    }

    private void ensureMutable(Finding finding) {
        if ("CLOSED".equals(finding.getRemediationStatus())) {
            throw new IllegalStateException("Closed findings cannot be changed");
        }
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("Actor is required");
        }
    }

    private String snapshot(Finding finding) {
        return "status=" + finding.getRemediationStatus()
                + ";owner=" + finding.getRemediationOwner()
                + ";dueDate=" + finding.getRemediationDueDate()
                + ";plan=" + finding.getRemediationPlan()
                + ";resolutionComment=" + finding.getResolutionComment()
                + ";resolvedAt=" + finding.getResolvedAt()
                + ";resolvedBy=" + finding.getResolvedBy()
                + ";workpaperId=" + (finding.getWorkpaper() == null ? null : finding.getWorkpaper().getId());
    }
}
