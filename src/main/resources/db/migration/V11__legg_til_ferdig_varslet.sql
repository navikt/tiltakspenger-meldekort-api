-- 1360 Flagg for å holde styr på om varslene har blitt inaktivert
ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS varsel_inaktivert BOOLEAN;
