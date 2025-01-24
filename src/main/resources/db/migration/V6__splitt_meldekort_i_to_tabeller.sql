DROP TABLE IF EXISTS meldekort;

CREATE TABLE meldeperiode
(
    id                            varchar PRIMARY KEY,
    kjede_id                      varchar     NOT NULL,
    versjon                       INTEGER     NOT NULL,
    sak_id                        varchar     NOT NULL,
    fnr                           varchar     NOT NULL,
    opprettet                     TIMESTAMPTZ NOT NULL,
    fra_og_med                    DATE        NOT NULL,
    til_og_med                    DATE        NOT NULL,
    maks_antall_dager_for_periode INTEGER     NOT NULL,
    gir_rett                      JSONB       NOT NULL,

    CONSTRAINT unique_kjede_id_opprettet UNIQUE (sak_id, kjede_id, opprettet),
    CONSTRAINT unique_kjede_id_versjon UNIQUE (sak_id, kjede_id, versjon)
);
CREATE INDEX idx_meldeperiode_sak_id ON meldeperiode (sak_id);
CREATE INDEX idx_meldeperiode_kjede_id ON meldeperiode (kjede_id);
CREATE INDEX idx_meldeperiode_periode ON meldeperiode (fra_og_med, til_og_med);

CREATE TABLE meldekort_bruker
(
    id                       varchar PRIMARY KEY,
    meldeperiode_id          varchar     NOT NULL REFERENCES meldeperiode (id),
    sak_id                   varchar     NOT NULL,
    mottatt                  TIMESTAMPTZ NULL,
    dager                    jsonb       NOT NULL,
    sendt_til_saksbehandling TIMESTAMPTZ NULL
);

CREATE INDEX idx_meldekort_bruker_sak_id ON meldekort_bruker (sak_id);
CREATE INDEX idx_meldekort_bruker_meldeperiode_id ON meldekort_bruker (meldeperiode_id);
