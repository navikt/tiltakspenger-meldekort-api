-- Contract-steg 3 (siste) i expand/contract-migreringen: dropp den gamle JSONB-kolonnen.
--
-- Meldeperiodebehandlingene bor nå i meldeperiodebehandling-tabellen. Etter Deploy B skriver og leser
-- ingen levende pod lenger meldekortvedtak.meldeperiodebehandlinger, så kolonnen kan trygt fjernes.
ALTER TABLE meldekortvedtak
    DROP COLUMN meldeperiodebehandlinger;
