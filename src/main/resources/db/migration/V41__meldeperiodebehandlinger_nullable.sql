-- Contract-steg 2 i expand/contract-migreringen: slutt å skrive til den gamle JSONB-kolonnen.
--
-- Etter Deploy A leser ingen levende pod lenger fra meldekortvedtak.meldeperiodebehandlinger - all lesing
-- skjer mot den nye meldeperiodebehandling-tabellen. Appen slutter nå å skrive til den gamle kolonnen.
--
-- Kolonnen var NOT NULL (V37). Vi dropper NOT NULL-kravet slik at nye inserts kan utelate den.
-- Forrige generasjon podder (Deploy A) dual-skriver fortsatt en ekte verdi i kolonnen mens denne deployen
-- ruller ut, og påvirkes ikke av at NOT NULL fjernes. Selve kolonnen droppes i et eget, senere steg.
ALTER TABLE meldekortvedtak
    ALTER COLUMN meldeperiodebehandlinger DROP NOT NULL;
