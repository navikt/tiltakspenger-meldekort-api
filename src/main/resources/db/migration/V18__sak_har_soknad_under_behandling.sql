ALTER TABLE sak
    ADD COLUMN IF NOT EXISTS har_soknad_under_behandling BOOLEAN not null default false;
