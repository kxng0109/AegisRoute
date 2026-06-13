CREATE TABLE transfer_saga_states
(
    id                    UUID                        NOT NULL,
    user_id               VARCHAR(255)                NOT NULL,
    amount                DECIMAL(19, 2)              NOT NULL,
    destination_account   VARCHAR(255)                NOT NULL,
    destination_bank_code VARCHAR(255)                NOT NULL,
    status                VARCHAR(255)                NOT NULL,
    ledger_reference_id   UUID                        NOT NULL,
    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_transfer_saga_states PRIMARY KEY (id)
);