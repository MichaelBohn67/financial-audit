package de.bohnottensen.financialaudit.application.usecase.workpaper;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.ReviewAction;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReviewActionRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.springframework.stereotype.Service;

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

    public Workpaper create(String title, String createdBy) {
        Workpaper workpaper = new Workpaper();
        workpaper.setTitle(title);
        workpaper.setStatus("DRAFT");
        workpaper.setCreatedBy(createdBy);
        Workpaper savedWorkpaper = workpaperRepository.save(workpaper);
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

    public Workpaper submit(Long workpaperId, String actor) {
        Workpaper workpaper = workpaperRepository.findById(workpaperId).orElseThrow();
        String previousValue = workpaperSnapshot(workpaper);
        workpaper.setStatus("SUBMITTED");
        workpaper.setAssignedTo(actor);
        Workpaper savedWorkpaper = workpaperRepository.save(workpaper);
        auditTrailWriter.record(
                "WORKPAPER",
                savedWorkpaper.getId(),
                "WORKPAPER_SUBMITTED",
                actor,
                "Workpaper submitted",
                previousValue,
                workpaperSnapshot(savedWorkpaper)
        );
        return savedWorkpaper;
    }

    public Workpaper approve(Long workpaperId, String actor) {
        Workpaper workpaper = workpaperRepository.findById(workpaperId).orElseThrow();
        String previousValue = workpaperSnapshot(workpaper);
        workpaper.setStatus("APPROVED");
        workpaper.setAssignedTo(actor);
        Workpaper savedWorkpaper = workpaperRepository.save(workpaper);
        auditTrailWriter.record(
                "WORKPAPER",
                savedWorkpaper.getId(),
                "WORKPAPER_APPROVED",
                actor,
                "Workpaper approved",
                previousValue,
                workpaperSnapshot(savedWorkpaper)
        );
        ReviewAction reviewAction = new ReviewAction();
        reviewAction.setWorkpaper(savedWorkpaper);
        reviewAction.setActor(actor);
        reviewAction.setAction("APPROVE");
        reviewActionRepository.save(reviewAction);
        return savedWorkpaper;
    }

    private String workpaperSnapshot(Workpaper workpaper) {
        return "title=" + workpaper.getTitle()
                + ";status=" + workpaper.getStatus()
                + ";createdBy=" + workpaper.getCreatedBy()
                + ";assignedTo=" + workpaper.getAssignedTo()
                + ";projectId=" + workpaper.getProjectId();
    }
}
