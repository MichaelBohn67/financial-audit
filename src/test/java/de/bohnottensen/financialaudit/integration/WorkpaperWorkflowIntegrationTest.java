package de.bohnottensen.financialaudit.integration;

import de.bohnottensen.financialaudit.application.usecase.workpaper.WorkpaperService;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import de.bohnottensen.financialaudit.domain.model.WorkpaperStatus;
import de.bohnottensen.financialaudit.infrastructure.persistence.AuditEventRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReviewActionRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test: full workpaper review workflow through WorkpaperService with real H2 DB.
 * Verifies state transitions, ReviewAction persistence, and AuditTrail events.
 */
@SpringBootTest
@Transactional
class WorkpaperWorkflowIntegrationTest {

    @Autowired
    private WorkpaperService workpaperService;

    @Autowired
    private WorkpaperRepository workpaperRepository;

    @Autowired
    private ReviewActionRepository reviewActionRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    @WithMockUser(username = "assistant", roles = "ASSISTANT")
    void shouldCompleteFullWorkflowAssistantToApproval() {
        long auditEventsBefore = auditEventRepository.count();

        // 1. Create
        Workpaper wp = workpaperService.create("AML-Audit Q1", "assistant");
        assertThat(wp.getId()).isNotNull();
        assertThat(wp.getStatus()).isEqualTo(WorkpaperStatus.DRAFT.name());

        Long id = wp.getId();

        // 2. Start progress
        wp = workpaperService.startProgress(id, "assistant");
        assertThat(wp.getStatus()).isEqualTo(WorkpaperStatus.IN_PROGRESS.name());

        // 3. Submit for review
        wp = workpaperService.submit(id, "assistant");
        assertThat(wp.getStatus()).isEqualTo(WorkpaperStatus.SUBMITTED.name());

        // 4. Senior requests changes
        wp = withRole("senior", "ROLE_SENIOR_AUDITOR",
                () -> workpaperService.requestChanges(id, "senior", "Need more evidence"));
        assertThat(wp.getStatus()).isEqualTo(WorkpaperStatus.CHANGES_REQUESTED.name());

        // 5. Assistant resumes and resubmits
        wp = workpaperService.startProgress(id, "assistant");
        wp = workpaperService.submit(id, "assistant");
        assertThat(wp.getStatus()).isEqualTo(WorkpaperStatus.SUBMITTED.name());

        // 6. Wirtschaftspruefer gives final approval
        wp = withRole("wirtschaftspruefer", "ROLE_WIRTSCHAFTSPRUEFER",
                () -> workpaperService.approve(id, "wirtschaftspruefer"));
        assertThat(wp.getStatus()).isEqualTo(WorkpaperStatus.APPROVED.name());

        // ReviewActions: CREATE, START, SUBMIT, REQUEST_CHANGES, START, SUBMIT, APPROVE = 7
        var actions = reviewActionRepository.findByWorkpaper(
                workpaperRepository.findById(id).orElseThrow());
        assertThat(actions).hasSize(7);
        assertThat(actions.stream().map(a -> a.getAction()).toList())
                .containsExactly("CREATE", "START", "SUBMIT", "REQUEST_CHANGES", "START", "SUBMIT", "APPROVE");

        // AuditTrail events must be written for each step (7 steps + CREATE = 7 total)
        assertThat(auditEventRepository.count()).isGreaterThan(auditEventsBefore + 6);
        assertThat(auditEventRepository.findAll()).anyMatch(e ->
                "WORKPAPER".equals(e.getEntityType())
                        && "WORKPAPER_APPROVE".equals(e.getEventType())
                        && "wirtschaftspruefer".equals(e.getActor()));
    }

    @Test
    @WithMockUser(username = "assistant", roles = "ASSISTANT")
    void shouldPersistWorkpaperInRepository() {
        workpaperService.create("Sampling Review", "assistant");
        workpaperService.create("AML Review", "assistant");

        assertThat(workpaperRepository.count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @WithMockUser(username = "assistant", roles = "ASSISTANT")
    void shouldRejectInvalidStateTransition() {
        Workpaper wp = workpaperService.create("Invalid Transition Test", "assistant");
        Long id = wp.getId();

        // Cannot approve directly from DRAFT
        assertThatThrownBy(() -> withRole("wirtschaftspruefer", "ROLE_WIRTSCHAFTSPRUEFER",
                () -> workpaperService.approve(id, "wirtschaftspruefer")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid workpaper transition");
    }

    @Test
    @WithMockUser(username = "assistant", roles = "ASSISTANT")
    void shouldRecordCorrectActorInEachReviewAction() {
        Workpaper wp = workpaperService.create("Actor Test WP", "assistant");
        Long id = wp.getId();
        workpaperService.startProgress(id, "assistant");
        workpaperService.submit(id, "assistant");
        withRole("senior", "ROLE_SENIOR_AUDITOR",
                () -> workpaperService.requestChanges(id, "senior", "More detail please"));

        var actions = reviewActionRepository.findByWorkpaper(
                workpaperRepository.findById(id).orElseThrow());

        long assistantActions = actions.stream().filter(a -> "assistant".equals(a.getActor())).count();
        long seniorActions = actions.stream().filter(a -> "senior".equals(a.getActor())).count();
        assertThat(assistantActions).isGreaterThanOrEqualTo(3); // CREATE, START, SUBMIT
        assertThat(seniorActions).isEqualTo(1); // REQUEST_CHANGES
    }

    /**
     * Run a callable with a temporary security context for a given user and role,
     * then restore the previous context.
     */
    private <T> T withRole(String username, String role, java.util.concurrent.Callable<T> callable) {
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                    username, null, List.of(new SimpleGrantedAuthority(role))));
            SecurityContextHolder.setContext(ctx);
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
