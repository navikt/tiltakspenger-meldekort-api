-- Vi trenger et flagg som er non-null for gammel og ny data.
ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS sendt_varsel boolean NOT NULL DEFAULT false;

UPDATE meldekort_bruker
SET sendt_varsel = true
WHERE varsel_id IS NOT NULL;

ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS sendt_varsel_tidspunkt TIMESTAMPTZ;

ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS sendt_varsel_json_request VARCHAR;

UPDATE meldekort_bruker
SET varsel_id = gen_random_uuid()
WHERE varsel_id IS NULL;

ALTER TABLE meldekort_bruker
    ALTER COLUMN varsel_id SET NOT NULL;