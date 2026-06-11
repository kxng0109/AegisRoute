CREATE TABLE accounts
(
    id         UUID           NOT NULL,
    user_id    VARCHAR(255)   NOT NULL,
    balance    DECIMAL(19, 2) NOT NULL,
    currency   VARCHAR(255)   NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

CREATE TABLE transaction_logs
(
    id             UUID           NOT NULL,
    account_id     UUID           NOT NULL,
    operation_type VARCHAR(255)   NOT NULL,
    amount         DECIMAL(19, 2) NOT NULL,
    reference_id   VARCHAR(255)   NOT NULL,
    status         VARCHAR(255)   NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_transaction_logs PRIMARY KEY (id)
);

ALTER TABLE accounts
    ADD CONSTRAINT uc_accounts_user UNIQUE (user_id);

ALTER TABLE transaction_logs
    ADD CONSTRAINT uc_transaction_logs_reference UNIQUE (reference_id);

ALTER TABLE transaction_logs
    ADD CONSTRAINT FK_TRANSACTION_LOGS_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES accounts (id);