-- Sammensatt indeks for de hyppige oppslagene "finnes det en behandling for denne kjeden i denne saken?"
-- (EXISTS/NOT EXISTS i meldekort-, landingsside-, microfrontend- og varsel-spørringene):
--   WHERE sak_id = :sak_id AND meldeperiode_kjede_id = :kjede_id
--
-- Indeksen dekker også oppslag på sak_id alene (ledende kolonne), f.eks. harInnsendteMeldekort, så den
-- tidligere enkeltkolonne-indeksen idx_meldeperiodebehandling_sak_id (V39) blir overflødig og fjernes.
CREATE INDEX idx_meldeperiodebehandling_sak_id_meldeperiode_kjede_id
    ON meldeperiodebehandling (sak_id, meldeperiode_kjede_id);

DROP INDEX idx_meldeperiodebehandling_sak_id;
