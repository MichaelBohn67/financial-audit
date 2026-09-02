package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.workpaper.WorkpaperService;
import de.bohnottensen.financialaudit.domain.model.ReviewAction;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/workpapers")
public class WorkpaperReviewApiController {

    private final WorkpaperService workpaperService;

    public WorkpaperReviewApiController(WorkpaperService workpaperService) {
        this.workpaperService = workpaperService;
    }

    @PostMapping
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #request.tenantId(), #request.projectId())")
    public ResponseEntity<WorkpaperView> create(@RequestBody CreateWorkpaperRequest request,
                                                @AuthenticationPrincipal UserDetails user) {
        Workpaper workpaper = workpaperService.create(request.title(), request.tenantId(), request.projectId(), user.getUsername());
        return ResponseEntity.ok(toView(workpaper));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public ResponseEntity<WorkpaperView> get(@PathVariable Long id, @RequestParam String tenantId, @RequestParam String projectId) {
        Workpaper workpaper = workpaperService.findById(id, tenantId, projectId);
        return ResponseEntity.ok(toView(workpaper));
    }

    @GetMapping("/{id}/actions")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public ResponseEntity<List<ReviewActionView>> reviewActions(@PathVariable Long id, @RequestParam String tenantId, @RequestParam String projectId) {
        workpaperService.findById(id, tenantId, projectId);
        List<ReviewAction> actions = workpaperService.findReviewActions(id, tenantId, projectId);
        return ResponseEntity.ok(actions.stream().map(this::toActionView).toList());
    }

    /** AUDITOR, LEAD_AUDITOR, ADMIN: start working on a workpaper */
    @PostMapping("/{id}/start")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public ResponseEntity<WorkpaperView> startProgress(@PathVariable Long id, @RequestParam String tenantId, @RequestParam String projectId,
                                                       @AuthenticationPrincipal UserDetails user) {
        workpaperService.findById(id, tenantId, projectId);
        Workpaper workpaper = workpaperService.startProgress(id, tenantId, projectId, user.getUsername());
        return ResponseEntity.ok(toView(workpaper));
    }

    /** AUDITOR, LEAD_AUDITOR, ADMIN: submit workpaper for review */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@scopeAccessPolicy.canAccessProject(authentication, #tenantId, #projectId)")
    public ResponseEntity<WorkpaperView> submit(@PathVariable Long id,
                                                @RequestParam String tenantId, @RequestParam String projectId,
                                                @AuthenticationPrincipal UserDetails user) {
        Workpaper workpaper = workpaperService.submit(id, tenantId, projectId, user.getUsername());
        return ResponseEntity.ok(toView(workpaper));
    }

    /** LEAD_AUDITOR, ADMIN: send workpaper back with requested changes */
    @PostMapping("/{id}/request-changes")
    @PreAuthorize("hasAnyRole('LEAD_AUDITOR', 'ADMIN')")
    public ResponseEntity<WorkpaperView> requestChanges(@PathVariable Long id,
                                                        @RequestParam String tenantId, @RequestParam String projectId,
                                                        @RequestBody RequestChangesRequest request,
                                                        @AuthenticationPrincipal UserDetails user) {
        Workpaper workpaper = workpaperService.requestChanges(id, tenantId, projectId, user.getUsername(), request.comment());
        return ResponseEntity.ok(toView(workpaper));
    }

    /** LEAD_AUDITOR, ADMIN: approve a submitted workpaper */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('LEAD_AUDITOR', 'ADMIN')")
    public ResponseEntity<WorkpaperView> approve(@PathVariable Long id,
                                                 @RequestParam String tenantId, @RequestParam String projectId,
                                                 @AuthenticationPrincipal UserDetails user) {
        Workpaper workpaper = workpaperService.approve(id, tenantId, projectId, user.getUsername());
        return ResponseEntity.ok(toView(workpaper));
    }

    @PostMapping("/{id}/sign-off")
    @PreAuthorize("hasAnyRole('LEAD_AUDITOR', 'ADMIN')")
    public ResponseEntity<WorkpaperView> signOff(@PathVariable Long id,
                                                 @RequestParam String tenantId, @RequestParam String projectId,
                                                 @AuthenticationPrincipal UserDetails user) {
        Workpaper workpaper = workpaperService.signOff(id, tenantId, projectId, user.getUsername());
        return ResponseEntity.ok(toView(workpaper));
    }

    private WorkpaperView toView(Workpaper workpaper) {
        return new WorkpaperView(
                workpaper.getId(),
                workpaper.getTitle(),
                workpaper.getStatus(),
                workpaper.getCreatedBy(),
                workpaper.getAssignedTo(),
                workpaper.getProjectId(),
                workpaper.getCreatedAt(),
                workpaper.getUpdatedAt()
        );
    }

    private ReviewActionView toActionView(ReviewAction action) {
        return new ReviewActionView(
                action.getId(),
                action.getActor(),
                action.getAction(),
                action.getComment(),
                action.getCreatedAt()
        );
    }

    public record CreateWorkpaperRequest(String title, String tenantId, String projectId) {}

    public record RequestChangesRequest(String comment) {}

    public record WorkpaperView(Long id, String title, String status, String createdBy,
                                String assignedTo, Long projectId,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record ReviewActionView(Long id, String actor, de.bohnottensen.financialaudit.domain.model.ReviewActionType action, String comment,
                                   LocalDateTime createdAt) {}
}
