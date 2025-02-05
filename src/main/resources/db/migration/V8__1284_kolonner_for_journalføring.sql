-- 1284 Legg til kolonner for å flagge om et meldekort har blitt journalført eller ei.
ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS journalpost_id          VARCHAR,
    ADD COLUMN IF NOT EXISTS journalføringstidspunkt TIMESTAMPTZ;

ALTER TABLE meldeperiode
    ADD COLUMN IF NOT EXISTS saksnummer VARCHAR;
