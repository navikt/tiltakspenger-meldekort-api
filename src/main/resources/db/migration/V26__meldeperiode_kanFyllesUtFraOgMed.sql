ALTER TABLE meldeperiode
    ADD COLUMN IF NOT EXISTS kan_fylles_ut_fra_og_med TIMESTAMP;

UPDATE meldeperiode
SET kan_fylles_ut_fra_og_med = (
    CASE
        WHEN EXISTS (SELECT 1
                     FROM meldekort_bruker mk
                     WHERE mk.meldeperiode_id = meldeperiode.id
                       AND mk.varsel_id IS NOT NULL)
            THEN (til_og_med::date - 2 || ' 00:00:00')::TIMESTAMP
        ELSE (til_og_med::date - 2 || ' 15:00:00')::TIMESTAMP
        END
    );

ALTER TABLE meldeperiode
    ALTER COLUMN kan_fylles_ut_fra_og_med SET NOT NULL;