-- Migrerer de gamle varsel-kolonnene fra meldekort_bruker til en egen arkivtabell.
-- Kolonnene (varsel_id, varsel_inaktivert, sendt_varsel, sendt_varsel_tidspunkt, sendt_varsel_json_request)
-- ble erstattet av den nye varsel-tabellen og brukes ikke lenger.

CREATE TABLE arkiv_varsel
(
    meldekort_id              VARCHAR PRIMARY KEY NOT NULL,
    varsel_id                 VARCHAR             NOT NULL,
    varsel_inaktivert         BOOLEAN,
    sendt_varsel              BOOLEAN             NOT NULL,
    sendt_varsel_tidspunkt    TIMESTAMPTZ,
    sendt_varsel_json_request VARCHAR
);

INSERT INTO arkiv_varsel (meldekort_id, varsel_id, varsel_inaktivert, sendt_varsel, sendt_varsel_tidspunkt, sendt_varsel_json_request)
SELECT id, varsel_id, varsel_inaktivert, sendt_varsel, sendt_varsel_tidspunkt, sendt_varsel_json_request
FROM meldekort_bruker;

ALTER TABLE meldekort_bruker
    DROP COLUMN varsel_id,
    DROP COLUMN varsel_inaktivert,
    DROP COLUMN sendt_varsel,
    DROP COLUMN sendt_varsel_tidspunkt,
    DROP COLUMN sendt_varsel_json_request;

-- Ny varsel-tabell frikoblet fra individuelle meldekort og knyttet til sak.
-- type-kolonnen lagrer tilstanden (SkalAktiveres, Aktiv, SkalInaktiveres, Inaktivert, Avbrutt) for enklere debugging.
CREATE TABLE varsel
(
    varsel_id                    VARCHAR PRIMARY KEY NOT NULL,
    sak_id                       VARCHAR             NOT NULL REFERENCES sak (id),
    type                         VARCHAR             NOT NULL,
    skal_aktiveres_tidspunkt     TIMESTAMPTZ         NOT NULL,
    skal_aktiveres_begrunnelse   VARCHAR             NOT NULL,
    aktiveringstidspunkt         TIMESTAMPTZ         NULL,
    skal_inaktiveres_tidspunkt   TIMESTAMPTZ         NULL,
    skal_inaktiveres_begrunnelse VARCHAR             NULL,
    inaktiveringstidspunkt       TIMESTAMPTZ         NULL,
    avbrutt_tidspunkt            TIMESTAMPTZ         NULL,
    avbrutt_begrunnelse          VARCHAR             NULL,
    aktiveringsmetadata          VARCHAR             NULL,
    inaktiveringsmetadata        VARCHAR             NULL,
    opprettet                    TIMESTAMPTZ         NOT NULL DEFAULT now(),
    sist_endret                  TIMESTAMPTZ         NOT NULL DEFAULT now()
);

CREATE INDEX idx_varsel_sak_id ON varsel (sak_id);

-- Indeks for aktiveringsjobben: SkalAktiveres der tidspunktet er passert
CREATE INDEX idx_varsel_skal_aktiveres
    ON varsel (skal_aktiveres_tidspunkt)
    WHERE type = 'SkalAktiveres';

-- Indeks for inaktiveringsjobben: SkalInaktiveres der tidspunktet er passert
CREATE INDEX idx_varsel_skal_inaktiveres
    ON varsel (skal_inaktiveres_tidspunkt)
    WHERE type = 'SkalInaktiveres';

-- Indeks for å finne aktive varsler per sak (for invariant: maks 1 aktivt varsel)
CREATE INDEX idx_varsel_aktiv_per_sak
    ON varsel (sak_id)
    WHERE type IN ('SkalAktiveres', 'Aktiv', 'SkalInaktiveres');

-- Nytt flagg på sak for å markere at varsel bør vurderes.
-- Default true slik at alle eksisterende saker evalueres ved førstegangskjøring.
ALTER TABLE sak
    ADD COLUMN skal_vurdere_varsel BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN sist_vurdert_varsel TIMESTAMPTZ NULL;

CREATE INDEX idx_sak_skal_vurdere_varsel
    ON sak (id)
    WHERE skal_vurdere_varsel = true;

