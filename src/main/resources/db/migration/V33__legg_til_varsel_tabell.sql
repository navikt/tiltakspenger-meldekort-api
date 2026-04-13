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
-- type-kolonnen lagrer tilstanden (SkalAktiveres, Aktiv, SkalInaktiveres, Inaktivert) for enklere debugging.
CREATE TABLE varsel
(
    varsel_id                    VARCHAR PRIMARY KEY NOT NULL,
    sak_id                       VARCHAR             NOT NULL REFERENCES sak (id),
    type                         VARCHAR             NOT NULL,
    skal_aktiveres_tidspunkt     TIMESTAMPTZ         NOT NULL,
    skal_aktiveres_eksternt_tidspunkt TIMESTAMPTZ  NOT NULL,
    skal_aktiveres_begrunnelse   VARCHAR             NOT NULL,
    aktiveringstidspunkt         TIMESTAMPTZ         NULL,
    ekstern_aktiveringstidspunkt TIMESTAMPTZ         NULL,
    skal_inaktiveres_tidspunkt   TIMESTAMPTZ         NULL,
    skal_inaktiveres_begrunnelse VARCHAR             NULL,
    inaktiveringstidspunkt       TIMESTAMPTZ         NULL,
    aktiveringsmetadata          VARCHAR             NULL,
    inaktiveringsmetadata        VARCHAR             NULL,
    opprettet                    TIMESTAMPTZ         NOT NULL,
    sist_endret                  TIMESTAMPTZ         NOT NULL
);

CREATE INDEX idx_varsel_sak_id ON varsel (sak_id);
CREATE UNIQUE INDEX idx_varsel_unik_opprettet_per_sak ON varsel (sak_id, opprettet);

-- Indeks for aktiveringsjobben: alle SkalAktiveres (Min side styrer selv utsatt levering, så vi
-- produserer Kafka-hendelsen så fort som mulig uavhengig av skal_aktiveres_tidspunkt).
CREATE INDEX idx_varsel_skal_aktiveres
    ON varsel (opprettet)
    WHERE type = 'SkalAktiveres';

-- Indeks for inaktiveringsjobben: SkalInaktiveres der tidspunktet er passert
CREATE INDEX idx_varsel_skal_inaktiveres
    ON varsel (skal_inaktiveres_tidspunkt)
    WHERE type = 'SkalInaktiveres';

-- Håndhever invarianten "maks ett pågående varsel (SkalAktiveres/Aktiv) per sak" direkte i databasen.
CREATE UNIQUE INDEX idx_varsel_unik_pågående_oppretting_per_sak
    ON varsel (sak_id)
    WHERE type IN ('SkalAktiveres', 'Aktiv');

-- Håndhever invarianten "maks ett varsel som skal inaktiveres per sak" direkte i databasen.
CREATE UNIQUE INDEX idx_varsel_unik_skal_inaktiveres_per_sak
    ON varsel (sak_id)
    WHERE type = 'SkalInaktiveres';

-- Nytt flagg på sak for å markere at varsel bør vurderes.
-- Default true slik at alle eksisterende saker evalueres ved førstegangskjøring.
-- sist_flagget_tidspunkt settes via clock_timestamp() ved hver flagging og brukes som
-- optimistisk lås i varseljobben (markerVarselVurdert) for å oppdage samtidige flagginger.
ALTER TABLE sak
    ADD COLUMN skal_vurdere_varsel BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN sist_vurdert_varsel TIMESTAMPTZ NULL,
    ADD COLUMN sist_flagget_tidspunkt TIMESTAMPTZ NULL;

-- VurderVarselService kjører hentSakerSomSkalVurdereVarsel hvert 10. sek.
-- Vi sorterer på sist_flagget_tidspunkt NULLS FIRST (first-flagged-first-served) for å unngå
-- sult og gi rettferdig kø. For at LIMIT N skal kunne plukke radene direkte fra indeksen uten
-- ekstra sortering, legger vi sist_flagget_tidspunkt først i indeksen. id legges med som
-- tiebreaker for deterministisk rekkefølge ved like tidspunkt (samt for index-only scan).
CREATE INDEX idx_sak_skal_vurdere_varsel_kø
    ON sak (sist_flagget_tidspunkt NULLS FIRST, id)
    WHERE skal_vurdere_varsel = true;
