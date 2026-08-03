package de.bohnottensen.financialaudit.application.usecase.workpaper;

import de.bohnottensen.financialaudit.domain.model.ReviewAction;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReviewActionRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.springframework.stereotype.Service;

@Service
public class WorkpaperService {

    private final WorkpaperRepository workpaperRepository;
    private final ReviewActionRepository reviewActionRepository;

    public WorkpaperService(WorkpaperRepository workpaperRepository, ReviewActionRepository reviewActionRepository) {
        this.workpaperRepository = workpaperRepository;
        this.reviewActionRepository = reviewActionRepository;
    }

    public Workpaper create(String title, String createdBy) {
        Workpaper workpaper = new Workpaper();
        workpaper.setTitle(title);
        workpaper.setStatus("DRAFT");
        workpaper.setCreatedBy(createdBy);
        return workpaperRepository.save(workpaper);
    }

    public Workpaper submit(Long workpaperId, String actor) {
        Workpaper workpaper = workpaperRepository.findById(workpaperId).orElseThrow();
        workpaper.setStatus("SUBMITTED");
        workpaper.setAssignedTo(actor);
        return workpaperRepository.save(workpaper);
    }

    public Workpaper approve(Long workpaperId, String actor) {
        Workpaper workpaper = workpaperRepository.findById(workpaperId).orElseThrow();
        workpaper.setStatus("APPROVED");
        workpaper.setAssignedTo(actor);
        workpaperRepository.save(workpaper);
        ReviewAction reviewAction = new ReviewAction();
        reviewAction.setWorkpaper(workpaper);
        reviewAction.setActor(actor);
        reviewAction.setAction("APPROVE");
        reviewActionRepository.save(reviewAction);
        return workpaper;
    }
}
