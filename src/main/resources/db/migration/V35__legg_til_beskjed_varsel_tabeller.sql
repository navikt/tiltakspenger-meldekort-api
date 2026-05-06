CREATE TABLE beskjed_varsel
(
    varsel_id         VARCHAR PRIMARY KEY NOT NULL,
    sak_id            VARCHAR             NOT NULL REFERENCES sak (id),
    sendingsmetadata  VARCHAR             NOT NULL,
    opprettet         TIMESTAMPTZ         NOT NULL
);

CREATE TABLE beskjed_varsel_meldeperiode
(
    varsel_id                VARCHAR NOT NULL REFERENCES beskjed_varsel (varsel_id),
    sak_id                   VARCHAR NOT NULL REFERENCES sak (id),
    meldeperiode_id          VARCHAR NOT NULL REFERENCES meldeperiode (id),
    kjede_id                 VARCHAR NOT NULL,
    versjon                  INTEGER NOT NULL,
    siste_innsendte_versjon  INTEGER NOT NULL,
    endringsmetadata         JSONB   NOT NULL,
    PRIMARY KEY (varsel_id, meldeperiode_id)
);

CREATE UNIQUE INDEX idx_beskjed_varsel_meldeperiode_unik_sak_kjede_versjon
    ON beskjed_varsel_meldeperiode (sak_id, kjede_id, versjon);


