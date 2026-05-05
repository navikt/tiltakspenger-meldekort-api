DO
$$
    BEGIN
        IF EXISTS (SELECT 1
                   FROM meldeperiode mp
                            LEFT JOIN sak s ON s.id = mp.sak_id
                   WHERE s.id IS NULL) THEN
            RAISE EXCEPTION 'Kan ikke legge FK fra meldeperiode.sak_id til sak.id. Fant meldeperioder uten sak.';
        END IF;

        IF EXISTS (SELECT 1
                   FROM meldekort_bruker mb
                            LEFT JOIN sak s ON s.id = mb.sak_id
                   WHERE s.id IS NULL) THEN
            RAISE EXCEPTION 'Kan ikke legge FK fra meldekort_bruker.sak_id til sak.id. Fant meldekort uten sak.';
        END IF;

        IF EXISTS (SELECT 1
                   FROM meldekort_bruker mb
                            JOIN meldeperiode mp ON mp.id = mb.meldeperiode_id
                   WHERE mb.sak_id <> mp.sak_id) THEN
            RAISE EXCEPTION 'Kan ikke gjøre sak til eier av fnr/saksnummer. Fant meldekort hvor sak_id avviker fra meldeperiode.sak_id.';
        END IF;

        IF EXISTS (SELECT 1
                   FROM meldeperiode mp
                            JOIN sak s ON s.id = mp.sak_id
                   WHERE mp.fnr <> s.fnr) THEN
            RAISE EXCEPTION 'Kan ikke droppe meldeperiode.fnr. Fant meldeperioder hvor meldeperiode.fnr avviker fra sak.fnr.';
        END IF;

        IF EXISTS (SELECT 1
                   FROM meldeperiode mp
                            JOIN sak s ON s.id = mp.sak_id
                   WHERE mp.saksnummer IS NOT NULL
                     AND mp.saksnummer <> s.saksnummer) THEN
            RAISE EXCEPTION 'Kan ikke droppe meldeperiode.saksnummer. Fant meldeperioder hvor meldeperiode.saksnummer avviker fra sak.saksnummer.';
        END IF;
    END;
$$;

ALTER TABLE meldeperiode
    ADD CONSTRAINT meldeperiode_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES sak (id);

ALTER TABLE meldekort_bruker
    ADD CONSTRAINT meldekort_bruker_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES sak (id);

DROP INDEX IF EXISTS idx_meldeperiode_fnr;

ALTER TABLE meldeperiode
    DROP COLUMN fnr,
    DROP COLUMN saksnummer;
