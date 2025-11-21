ALTER TABLE meldeperiode
    ADD COLUMN kan_fylles_ut_fra_og_med TIMESTAMP;

UPDATE meldeperiode
SET kan_fylles_ut_fra_og_med = (til_og_med || ' 00:00:00')::TIMESTAMP;

ALTER TABLE meldeperiode
    ALTER COLUMN kan_fylles_ut_fra_og_med SET NOT NULL;