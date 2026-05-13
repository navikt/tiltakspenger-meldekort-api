-- Fjern redundante indekser:
-- idx_sak_fnr dekkes allerede av UNIQUE-constraint sak_fnr_key.
-- idx_meldeperiode_sak_id dekkes av venstreprefiks i idx_meldeperiode_sak_id_kjede_id_versjon.
-- idx_meldeperiode_sak_id_kjede_id_versjon dekkes av "CONSTRAINT unique_kjede_id_versjon UNIQUE (sak_id, kjede_id, versjon)"
-- varsel.sak_id trenger ingen egen indeks – idx_varsel_unik_opprettet_per_sak (sak_id, opprettet)
--   dekker venstreprefiks sak_id og brukes for FK-oppslag/joins.
DROP INDEX IF EXISTS idx_sak_fnr;
DROP INDEX IF EXISTS idx_meldeperiode_sak_id;
DROP INDEX IF EXISTS idx_meldeperiode_sak_id_kjede_id_versjon;


-- Partiell indeks for køen i hentMeldekortForSendingTilSaksbehandling.
-- Spørringen sorterer på mottatt DESC og filtrerer på de samme predikatene.
CREATE INDEX IF NOT EXISTS idx_meldekort_bruker_klar_for_saksbehandling
    ON meldekort_bruker (mottatt DESC)
    WHERE sendt_til_saksbehandling IS NULL
      AND journalpost_id IS NOT NULL
      AND mottatt IS NOT NULL;

-- Partiell indeks for køen i hentDeSomSkalJournalføres.
CREATE INDEX IF NOT EXISTS idx_meldekort_bruker_skal_journalfores
    ON meldekort_bruker (mottatt)
    WHERE journalpost_id IS NULL
      AND mottatt IS NOT NULL;

-- Re-flagg alle saker for varselvurdering slik at den nye logikken
-- (eksludering basert på meldekortvedtak) plukkes opp for hele bestanden.
UPDATE sak
SET skal_vurdere_varsel = true,
    sist_flagget_tidspunkt = clock_timestamp();

