ALTER TABLE transfer_saga_states
    ADD CONSTRAINT uc_transfer_saga_states_ledger_reference UNIQUE (ledger_reference_id);

ALTER TABLE transfer_saga_states
    DROP COLUMN ledger_reference_id;

ALTER TABLE transfer_saga_states
    ADD ledger_reference_id VARCHAR(255) NOT NULL;

ALTER TABLE transfer_saga_states
    ADD CONSTRAINT uc_transfer_saga_states_ledger_reference UNIQUE (ledger_reference_id);