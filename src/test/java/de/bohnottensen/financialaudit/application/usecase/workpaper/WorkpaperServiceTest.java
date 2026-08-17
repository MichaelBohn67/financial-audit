package de.bohnottensen.financialaudit.application.usecase.workpaper;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.ReviewAction;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import de.bohnottensen.financialaudit.infrastructure.persistence.ReviewActionRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.WorkpaperRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkpaperServiceTest {

    @Test
    void shouldFollowValidStateTransitionsAndPersistReviewActions() {
        WorkpaperRepository workpaperRepository = mock(WorkpaperRepository.class);
        ReviewActionRepository reviewActionRepository = mock(ReviewActionRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        Workpaper workpaper = new Workpaper();
        workpaper.setId(10L);
        workpaper.setTitle("WP-1");
        workpaper.setStatus("DRAFT");
        workpaper.setCreatedBy("assistant");

        when(workpaperRepository.save(any(Workpaper.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workpaperRepository.findById(anyLong())).thenAnswer(invocation -> Optional.of(workpaper));

        List<ReviewAction> actions = new ArrayList<>();
        when(reviewActionRepository.save(any(ReviewAction.class))).thenAnswer(invocation -> {
            ReviewAction action = invocation.getArgument(0);
            actions.add(action);
            return action;
        });

        WorkpaperService service = new WorkpaperService(workpaperRepository, reviewActionRepository, auditTrailWriter);

        service.startProgress(10L, "assistant");
        service.submit(10L, "assistant");
        service.requestChanges(10L, "senior", "Need more evidence");
        service.startProgress(10L, "assistant");
        service.submit(10L, "assistant");
        service.approve(10L, "wirtschaftspruefer");
        service.signOff(10L, "wirtschaftspruefer");

        assertThat(workpaper.getStatus()).isEqualTo("SIGNED_OFF");
        assertThat(actions.stream().map(ReviewAction::getAction).toList())
                .containsExactly("START", "SUBMIT", "REQUEST_CHANGES", "START", "SUBMIT", "APPROVE", "SIGN_OFF");
    }

    @Test
    void shouldRejectInvalidStateTransition() {
        WorkpaperRepository workpaperRepository = mock(WorkpaperRepository.class);
        ReviewActionRepository reviewActionRepository = mock(ReviewActionRepository.class);
        AuditTrailWriter auditTrailWriter = mock(AuditTrailWriter.class);

        Workpaper workpaper = new Workpaper();
        workpaper.setId(11L);
        workpaper.setStatus("DRAFT");

        when(workpaperRepository.findById(11L)).thenReturn(Optional.of(workpaper));

        WorkpaperService service = new WorkpaperService(workpaperRepository, reviewActionRepository, auditTrailWriter);

        assertThatThrownBy(() -> service.approve(11L, "senior"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid workpaper transition: DRAFT -> APPROVED");
    }
}
