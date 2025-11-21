WITH meldekort_i_kjede AS (
    SELECT mb.id,
           row_number() OVER (PARTITION BY mp.sak_id, mp.kjede_id ORDER BY mb.mottatt, mb.id) AS indeks
    FROM meldekort_bruker mb
    JOIN meldeperiode mp ON mp.id = mb.meldeperiode_id
    WHERE mb.mottatt IS NOT NULL
)
UPDATE meldekort_bruker mb
SET korrigering = true
FROM meldekort_i_kjede meldekort
WHERE mb.id = meldekort.id
  AND meldekort.indeks > 1;
