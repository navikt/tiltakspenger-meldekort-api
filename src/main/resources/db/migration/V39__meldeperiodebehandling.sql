-- Trekker meldeperiodebehandlingene ut av JSONB-kolonnen meldekortvedtak.meldeperiodebehandlinger
-- og inn i en egen tabell. Gjør spørringer og manuell debugging av en enkelt meldeperiode enklere.
-- dager beholdes som JSONB på behandlingsraden (uendret per-dag-format).
--
-- Expand-steg i en expand/contract-migrering for rullende deploy:
-- Her opprettes KUN tabellen. Appen begynner å dual-skrive (gammel JSONB-kolonne + ny tabell),
-- men leser fortsatt fra den gamle kolonnen, slik at gamle podder kan kjøre side om side med nye.
--
-- Backfill av eksisterende/historiske rader gjøres bevisst IKKE her, men i en senere migrering.
-- Grunn: gamle podder som inserter etter at denne migreringen har kjørt (men før de er rullet ut)
-- skriver kun til den gamle kolonnen. En backfill her ville derfor uansett vært ufullstendig og måtte
-- gjentas. Reconcile-backfillen (idempotent, ON CONFLICT DO NOTHING) tas når alle gamle podder er ute,
-- rett før lesing flyttes til den nye tabellen. DROP COLUMN gjøres i et eget, senere steg.
CREATE TABLE meldeperiodebehandling
(
    meldekortvedtak_id    VARCHAR NOT NULL REFERENCES meldekortvedtak (id),
    sak_id                VARCHAR NOT NULL REFERENCES sak (id),
    meldeperiode_id       VARCHAR NOT NULL REFERENCES meldeperiode (id),
    meldeperiode_kjede_id VARCHAR NOT NULL,
    brukers_meldekort_id  VARCHAR REFERENCES meldekort_bruker (id),
    fra_og_med            DATE    NOT NULL,
    til_og_med            DATE    NOT NULL,
    dager                 JSONB   NOT NULL,
    PRIMARY KEY (meldekortvedtak_id, meldeperiode_kjede_id)
);

-- Indeks per fremmednøkkel. (meldekortvedtak_id dekkes av primærnøkkelens ledende kolonne.)
CREATE INDEX idx_meldeperiodebehandling_sak_id ON meldeperiodebehandling (sak_id);
CREATE INDEX idx_meldeperiodebehandling_meldeperiode_id ON meldeperiodebehandling (meldeperiode_id);
CREATE INDEX idx_meldeperiodebehandling_brukers_meldekort_id ON meldeperiodebehandling (brukers_meldekort_id);
