-- The fingerprint of the data a plan was made from, and its days, to warn when they change (D23, D125).
-- Proposals saved before this migration have none of the three and never show the warning.
ALTER TABLE plan_proposal
    ADD COLUMN inputs_hash varchar(64),
    ADD COLUMN first_day   date,
    ADD COLUMN last_day    date,
    ADD CONSTRAINT plan_proposal_inputs_check CHECK (
        (inputs_hash IS NULL AND first_day IS NULL AND last_day IS NULL)
        OR
        (inputs_hash IS NOT NULL AND first_day IS NOT NULL AND last_day IS NOT NULL AND first_day <= last_day)
    );
