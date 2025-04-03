ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS deaktivert TIMESTAMPTZ;
