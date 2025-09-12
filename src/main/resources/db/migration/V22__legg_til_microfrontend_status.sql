-- Erstatter microfrontend_inaktivert med microfrontend_status, for å kunne skille på hvorvidt vi har forsøkt å aktivere microfrontend for en sak ennå eller ei
ALTER TABLE sak
    DROP COLUMN IF EXISTS microfrontend_inaktivert,
    ADD COLUMN IF NOT EXISTS microfrontend_status TEXT DEFAULT 'UBEHANDLET' NOT NULL;
