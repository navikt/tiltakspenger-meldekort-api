-- 1310 Legg til kolonne for å kunne deaktivere varsel på min side for et meldekort.
ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS varsel_id VARCHAR;
