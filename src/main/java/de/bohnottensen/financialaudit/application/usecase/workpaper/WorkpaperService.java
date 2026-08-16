package de.bohnottensen.financialaudit.application.usecase.workpaper;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.ReviewAction;
import de.bohnottensen.financialaudit.domain.model.ReviewActionType;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import de.bohnottensen.financialaudit.domain.model.WorkpaperStatus;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReviewActionRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class WorkpaperService {

    private final WorkpaperRepository workpaperRepository;
    private final ReviewActionRepository reviewActionRepository;
    private final AuditTrailWriter auditTrailWriter;

    public WorkpaperService(WorkpaperRepository workpaperRepository,
                            ReviewActionRepository reviewActionRepository,
                            AuditTrailWriter auditTrailWriter) {
        this.workpaperRepository = workpaperRepository;
        this.reviewActionRepository = reviewActionRepository;
        this.auditTrailWriter = auditTrailWriter;
    }

    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN', 'ASSISTANT', 'SENIOR_AUDITOR', 'WIRTSCHAFTSPRUEFER')")
    public Workpaper create(String title, String createdBy) {
        Workpaper workpaper = new Workpaper();
        workpaper.setTitle(title);
        workpaper.setStatus(WorkpaperStatus.DRAFT.name());
        workpaper.setCreatedBy(createdBy);
        Workpaper savedWorkpaper = workpaperRepository.save(workpaper);
        recordReviewAction(savedWorkpaper, createdBy, ReviewActionType.CREATE, "Workpaper created");
        auditTrailWriter.record(
                "WORKPAPER",
                savedWorkpaper.getId(),
                "WORKPAPER_CREATED",
                createdBy,
                "Workpaper created",
                null,
                workpaperSnapshot(savedWorkpaper)
        );
        return savedWorkpaper;
    }

    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN', 'ASSISTANT', 'SENIOR_AUDITOR', 'WIRTSCHAFTSPRUEFER')")
    public Workpaper startProgress(Long workpaperId, String actor) {
        return transition(workpaperId, actor, ReviewActionType.START, WorkpaperStatus.IN_PROGRESS, "Workpaper progress started");
    }

    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN', 'ASSISTANT', 'SENIOR_AUDITOR', 'WIRTSCHAFTSPRUEFER')")
    public Workpaper submit(Long workpaperId, String actor) {
        return transition(workpaperId, actor, ReviewActionType.SUBMIT, WorkpaperStatus.SUBMITTED, "Workpaper submitted");
    }

    @PreAuthorize("hasAnyRole('LEAD_AUDITOR', 'ADMIN', 'SENIOR_AUDITOR', 'WIRTSCHAFTSPRUEFER')")
    public Workpaper requestChanges(Long workpaperId, String actor, String comment) {
        return transition(workpaperId, actor, ReviewActionType.REQUEST_CHANGES, WorkpaperStatus.CHANGES_REQUESTED, comment);
    }

    @PreAuthorize("hasAnyRole('LEAD_AUDITOR', 'ADMIN', 'WIRTSCHAFTSPRUEFER')")
    public Workpaper approve(Long workpaperId, String actor) {
        return transition(workpaperId, actor, ReviewActionType.APPROVE, WorkpaperStatus.APPROVED, "Workpaper approved");
    }

    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN', 'ASSISTANT', 'SENIOR_AUDITOR', 'WIRTSCHAFTSPRUEFER')")
    public Workpaper findById(Long workpaperId) {
        return workpaperRepository.findById(workpaperId).orElseThrow();
    }

    @PreAuthorize("hasAnyRole('AUDITOR', 'LEAD_AUDITOR', 'ADMIN', 'ASSISTANT', 'SENIOR_AUDITOR', 'WIRTSCHAFTSPRUEFER')")
    public List<ReviewAction> findReviewActions(Long workpaperId) {
        Workpaper workpaper = workpaperRepository.findById(workpaperId).orElseThrow();
        return reviewActionRepository.findByWorkpaper(workpaper);
    }

    private Workpaper transition(Long workpaperId,
                                 String actor,
                                 ReviewActionType action,
                                 WorkpaperStatus targetStatus,
                                 String summary) {
        Workpaper workpaper = workpaperRepository.findById(workpaperId).orElseThrow();
        WorkpaperStatus currentStatus = WorkpaperStatus.valueOf(workpaper.getStatus());
        validateTransition(currentStatus, targetStatus);

        String previousValue = workpaperSnapshot(workpaper);
        workpaper.setStatus(targetStatus.name());
        workpaper.setAssignedTo(actor);
        Workpaper savedWorkpaper = workpaperRepository.save(workpaper);

        auditTrailWriter.record(
                "WORKPAPER",
                savedWorkpaper.getId(),
                "WORKPAPER_" + action.name(),
                actor,
                summary,
                previousValue,
                workpaperSnapshot(savedWorkpaper)
        );

        recordReviewAction(savedWorkpaper, actor, action, summary);
        return savedWorkpaper;
    }

    private void validateTransition(WorkpaperStatus currentStatus, WorkpaperStatus targetStatus) {
        if (currentStatus == targetStatus) {
            throw new IllegalStateException("Workpaper is already in status " + targetStatus);
        }

        Set<WorkpaperStatus> allowedTargets;
        switch (currentStatus) {
            case DRAFT -> allowedTargets = EnumSet.of(WorkpaperStatus.IN_PROGRESS);
            case IN_PROGRESS -> allowedTargets = EnumSet.of(WorkpaperStatus.SUBMITTED);
            case SUBMITTED -> allowedTargets = EnumSet.of(WorkpaperStatus.CHANGES_REQUESTED, WorkpaperStatus.APPROVED);
            case CHANGES_REQUESTED -> allowedTargets = EnumSet.of(WorkpaperStatus.IN_PROGRESS);
            case APPROVED -> allowedTargets = EnumSet.noneOf(WorkpaperStatus.class);
            default -> throw new IllegalStateException("Unsupported status: " + currentStatus);
        }

        if (!allowedTargets.contains(targetStatus)) {
            throw new IllegalStateException("Invalid workpaper transition: " + currentStatus + " -> " + targetStatus);
        }
    }

    private void recordReviewAction(Workpaper workpaper,
                                    String actor,
                                    ReviewActionType action,
                                    String comment) {
        ReviewAction reviewAction = new ReviewAction();
        reviewAction.setWorkpaper(workpaper);
        reviewAction.setActor(actor);
        reviewAction.setAction(action.name());
        reviewAction.setComment(comment);
        reviewActionRepository.save(reviewAction);
    }

    private String workpaperSnapshot(Workpaper workpaper) {
        return "title=" + workpaper.getTitle()
                + ";status=" + workpaper.getStatus()
                + ";createdBy=" + workpaper.getCreatedBy()
                + ";assignedTo=" + workpaper.getAssignedTo()
                + ";projectId=" + workpaper.getProjectId();
    }
}
