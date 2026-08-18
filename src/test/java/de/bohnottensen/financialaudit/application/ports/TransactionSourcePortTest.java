package de.bohnottensen.financialaudit.application.ports;

import de.bohnottensen.financialaudit.domain.model.Booking;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionSourcePortTest {

    @Test
    void defaultSourceTypeUsesImplementingClassSimpleName() {
        TransactionSourcePort source = new NamedTransactionSource();

        assertThat(source.sourceType()).isEqualTo("NamedTransactionSource");
    }

    @Test
    void defaultSupportsAcceptsAnySource() {
        TransactionSourcePort source = new NamedTransactionSource();

        assertThat(source.supports(new Object())).isTrue();
    }

    private static final class NamedTransactionSource implements TransactionSourcePort {
        @Override
        public List<Booking> importTransactions(Object source) {
            return List.of();
        }
    }
}
