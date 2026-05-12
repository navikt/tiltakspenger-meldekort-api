-- Iverksatte meldekortvedtak mottatt fra saksbehandling-api.
-- Vedtak er immutable etter iverksettelse og dedupliseres på id.
-- Meldeperiodebehandlingene lagres som JSONB direkte på vedtaket.
CREATE TABLE meldekortvedtak
(
    id                       VARCHAR PRIMARY KEY NOT NULL,
    sak_id                   VARCHAR             NOT NULL REFERENCES sak (id),
    opprettet                TIMESTAMPTZ         NOT NULL,
    er_korrigering           BOOLEAN             NOT NULL,
    er_automatisk_behandlet  BOOLEAN             NOT NULL,
    meldeperiodebehandlinger JSONB               NOT NULL
);

CREATE INDEX idx_meldekortvedtak_sak_id ON meldekortvedtak (sak_id);
