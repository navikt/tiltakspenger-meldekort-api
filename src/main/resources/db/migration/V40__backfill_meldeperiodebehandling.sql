-- Contract-steg 1 i expand/contract-migreringen: reconcile-backfill.
--
-- V39 opprettet meldeperiodebehandling-tabellen, og appen har siden dual-skrevet både til den gamle
-- JSONB-kolonnen meldekortvedtak.meldeperiodebehandlinger og til den nye tabellen. Alle gamle podder
-- som kun skrev til JSONB-kolonnen er nå ute av drift, så det finnes ikke lenger skrivere som lager
-- rader uten tilsvarende rad i den nye tabellen.
--
-- Her fyller vi på historiske/eksisterende vedtak som ble lagret før dual-skrivingen begynte (samt
-- ev. rader fra gamle podder underveis i forrige utrulling). Vi pakker ut JSONB-arrayet til én rad
-- per meldeperiodebehandling. dager-formatet er identisk i gammel og ny representasjon, så det
-- kopieres direkte som JSONB.
--
-- Idempotent via ON CONFLICT DO NOTHING: rader som allerede er dual-skrevet hoppes over, slik at
-- migreringen trygt kan kjøres på nytt og ikke kolliderer med samtidige inserts fra nye podder.
INSERT INTO meldeperiodebehandling (meldekortvedtak_id,
                                    sak_id,
                                    meldeperiode_id,
                                    meldeperiode_kjede_id,
                                    brukers_meldekort_id,
                                    fra_og_med,
                                    til_og_med,
                                    dager)
SELECT mv.id,
       mv.sak_id,
       behandling ->> 'meldeperiodeId',
       behandling ->> 'meldeperiodeKjedeId',
       behandling ->> 'brukersMeldekortId',
       (behandling ->> 'fraOgMed')::date,
       (behandling ->> 'tilOgMed')::date,
       behandling -> 'dager'
FROM meldekortvedtak mv
         CROSS JOIN LATERAL jsonb_array_elements(mv.meldeperiodebehandlinger) AS behandling
ON CONFLICT (meldekortvedtak_id, meldeperiode_kjede_id) DO NOTHING;
