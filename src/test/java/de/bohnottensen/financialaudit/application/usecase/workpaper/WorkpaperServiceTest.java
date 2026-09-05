package de.bohnottensen.financialaudit.application.usecase.workpaper;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.ReviewAction;
import de.bohnottensen.financialaudit.domain.model.ReviewActionType;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReviewActionRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class WorkpaperServiceTest {

    private WorkpaperRepository workpaperRepository;
    private ReviewActionRepository reviewActionRepository;
    private AuditTrailWriter auditTrailWriter;
    private WorkpaperService service;

    @BeforeEach
    void setUp() {
        workpaperRepository = mock(WorkpaperRepository.class);
        reviewActionRepository = mock(ReviewActionRepository.class);
        auditTrailWriter = mock(AuditTrailWriter.class);
        service = new WorkpaperService(workpaperRepository, reviewActionRepository, auditTrailWriter);
    }

    @Test
    void shouldCreateWorkpaperAndRecordReviewActionAndAuditTrail() {
        when(workpaperRepository.save(any(Workpaper.class))).thenAnswer(invocation -> {
            Workpaper wp = invocation.getArgument(0);
            wp.setId(100L);
            return wp;
        });

        Workpaper created = service.create("Revenue Audit", "lead_auditor");

        assertThat(created.getId()).isEqualTo(100L);
        assertThat(created.getTitle()).isEqualTo("Revenue Audit");
        assertThat(created.getStatus()).isEqualTo("DRAFT");
        assertThat(created.getCreatedBy()).isEqualTo("lead_auditor");

        ArgumentCaptor<ReviewAction> actionCaptor = ArgumentCaptor.forClass(ReviewAction.class);
        verify(reviewActionRepository).save(actionCaptor.capture());
        ReviewAction action = actionCaptor.getValue();
        assertThat(action.getWorkpaper()).isSameAs(created);
        assertThat(action.getActor()).isEqualTo("lead_auditor");
        assertThat(action.getAction()).isEqualTo(ReviewActionType.CREATE);
        assertThat(action.getComment()).isEqualTo("Workpaper created");

        verify(auditTrailWriter).record(
                eq("WORKPAPER"),
                eq(100L),
                eq("WORKPAPER_CREATED"),
                eq("lead_auditor"),
                eq("Workpaper created"),
                isNull(),
                contains("title=Revenue Audit;status=DRAFT;createdBy=lead_auditor")
        );
    }

    @Test
    void shouldFollowValidStateTransitionsAndPersistReviewActions() {
        Workpaper workpaper = new Workpaper();
        workpaper.setId(10L);
        workpaper.setTitle("WP-1");
        workpaper.setStatus("DRAFT");
        workpaper.setCreatedBy("assistant");

        when(workpaperRepository.save(any(Workpaper.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workpaperRepository.findById(10L)).thenReturn(Optional.of(workpaper));

        List<ReviewAction> actions = new ArrayList<>();
        when(reviewActionRepository.save(any(ReviewAction.class))).thenAnswer(invocation -> {
            ReviewAction action = invocation.getArgument(0);
            actions.add(action);
            return action;
        });

        service.startProgress(10L, "assistant");
        assertThat(workpaper.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(workpaper.getAssignedTo()).isEqualTo("assistant");

        service.submit(10L, "assistant");
        assertThat(workpaper.getStatus()).isEqualTo("SUBMITTED");

        service.requestChanges(10L, "senior", "Need more evidence");
        assertThat(workpaper.getStatus()).isEqualTo("CHANGES_REQUESTED");

        service.startProgress(10L, "assistant");
        assertThat(workpaper.getStatus()).isEqualTo("IN_PROGRESS");

        service.submit(10L, "assistant");
        assertThat(workpaper.getStatus()).isEqualTo("SUBMITTED");

        service.approve(10L, "wirtschaftspruefer");
        assertThat(workpaper.getStatus()).isEqualTo("APPROVED");

        service.signOff(10L, "wirtschaftspruefer");
        assertThat(workpaper.getStatus()).isEqualTo("SIGNED_OFF");

        assertThat(actions.stream().map(ReviewAction::getAction).toList())
                .containsExactly(ReviewActionType.START, ReviewActionType.SUBMIT, ReviewActionType.REQUEST_CHANGES,
                        ReviewActionType.START, ReviewActionType.SUBMIT, ReviewActionType.APPROVE, ReviewActionType.SIGN_OFF);

        verify(auditTrailWriter, times(7)).record(
                eq("WORKPAPER"),
                eq(10L),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void shouldRejectInvalidStateTransitions() {
        Workpaper workpaper = new Workpaper();
        workpaper.setId(11L);
        workpaper.setStatus("DRAFT");
        when(workpaperRepository.findById(11L)).thenReturn(Optional.of(workpaper));

        assertThatThrownBy(() -> service.approve(11L, "senior"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid workpaper transition: DRAFT -> APPROVED");

        assertThatThrownBy(() -> service.submit(11L, "senior"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid workpaper transition: DRAFT -> SUBMITTED");

        // Same status transition
        workpaper.setStatus("IN_PROGRESS");
        assertThatThrownBy(() -> service.startProgress(11L, "senior"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Workpaper is already in status IN_PROGRESS");

        // From SIGNED_OFF
        workpaper.setStatus("SIGNED_OFF");
        assertThatThrownBy(() -> service.startProgress(11L, "senior"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid workpaper transition: SIGNED_OFF -> IN_PROGRESS");
    }

    @Test
    void shouldFindByIdAndFindReviewActions() {
        Workpaper workpaper = new Workpaper();
        workpaper.setId(20L);
        when(workpaperRepository.findById(20L)).thenReturn(Optional.of(workpaper));
        when(workpaperRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(service.findById(20L)).isSameAs(workpaper);
        assertThatThrownBy(() -> service.findById(999L)).isInstanceOf(NoSuchElementException.class);

        ReviewAction a1 = new ReviewAction();
        a1.setComment("Action 1");
        ReviewAction a2 = new ReviewAction();
        a2.setComment("Action 2");
        when(reviewActionRepository.findByWorkpaper(workpaper)).thenReturn(List.of(a1, a2));

        List<ReviewAction> actions = service.findReviewActions(20L);
        assertThat(actions).containsExactly(a1, a2);
    }

    @Test
    void shouldCreateAndTransitionScopedWorkpaper() {
        when(workpaperRepository.save(any(Workpaper.class))).thenAnswer(invocation -> {
            Workpaper wp = invocation.getArgument(0);
            if (wp.getId() == null) wp.setId(30L);
            return wp;
        });

        assertThatThrownBy(() -> service.create("Title", "", "PROJ", "user"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("Title", "TENANT", null, "user"))
                .isInstanceOf(IllegalArgumentException.class);

        Workpaper created = service.create("Scoped Title", "TENANT-1", "PROJECT-1", "user1");
        assertThat(created.getId()).isEqualTo(30L);
        assertThat(created.getTenantId()).isEqualTo("TENANT-1");
        assertThat(created.getScopeProjectId()).isEqualTo("PROJECT-1");

        when(workpaperRepository.findByIdAndTenantIdAndScopeProjectId(30L, "TENANT-1", "PROJECT-1"))
                .thenReturn(Optional.of(created));

        Workpaper inProgress = service.startProgress(30L, "TENANT-1", "PROJECT-1", "user1");
        assertThat(inProgress.getStatus()).isEqualTo("IN_PROGRESS");

        Workpaper submitted = service.submit(30L, "TENANT-1", "PROJECT-1", "user1");
        assertThat(submitted.getStatus()).isEqualTo("SUBMITTED");

        Workpaper changes = service.requestChanges(30L, "TENANT-1", "PROJECT-1", "senior", "Fix");
        assertThat(changes.getStatus()).isEqualTo("CHANGES_REQUESTED");

        service.startProgress(30L, "TENANT-1", "PROJECT-1", "user1");
        service.submit(30L, "TENANT-1", "PROJECT-1", "user1");
        Workpaper approved = service.approve(30L, "TENANT-1", "PROJECT-1", "lead");
        assertThat(approved.getStatus()).isEqualTo("APPROVED");

        Workpaper signedOff = service.signOff(30L, "TENANT-1", "PROJECT-1", "lead");
        assertThat(signedOff.getStatus()).isEqualTo("SIGNED_OFF");

        assertThat(service.findById(30L, "TENANT-1", "PROJECT-1")).isSameAs(created);
        ReviewAction action = new ReviewAction();
        when(reviewActionRepository.findByWorkpaper(created)).thenReturn(List.of(action));
        assertThat(service.findReviewActions(30L, "TENANT-1", "PROJECT-1")).containsExactly(action);
    }
}
