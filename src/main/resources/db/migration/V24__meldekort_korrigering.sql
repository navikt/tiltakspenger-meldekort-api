ALTER TABLE meldekort_bruker
  ADD COLUMN IF NOT EXISTS korrigering BOOLEAN NOT NULL DEFAULT FALSE;

WITH meldekort_i_kjede AS (
    SELECT id,
           row_number() OVER (PARTITION BY meldeperiode_kjede_id ORDER BY mottatt) AS index
    FROM meldekort_bruker
    WHERE mottatt IS NOT NULL
)
UPDATE meldekort_bruker mb
SET korrigering = true
FROM meldekort_i_kjede meldekort
WHERE mb.id = meldekort.id
  AND meldekort.index > 1;
