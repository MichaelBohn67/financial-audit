CREATE TABLE bookings
(
    id                    BIGINT AUTO_INCREMENT NOT NULL,
    foreign_transaction_id BIGINT,
    `description`         VARCHAR(255)          NOT NULL,
    amount                DECIMAL               NOT NULL,
    currency              VARCHAR(255)          NOT NULL,
    transaction_timestamp datetime(6)           NOT NULL,
    source_account        VARCHAR(255)          NOT NULL,
    destination_account   VARCHAR(255)          NOT NULL,
    created_at            datetime(6)           NOT NULL,
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT uq_bookings_foreign_transaction_id UNIQUE (foreign_transaction_id)
);