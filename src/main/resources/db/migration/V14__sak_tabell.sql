CREATE TABLE sak
(
    id                     VARCHAR PRIMARY KEY,
    fnr                    VARCHAR NOT NULL UNIQUE,
    saksnummer             VARCHAR NOT NULL UNIQUE,
    innvilgelsesperioder   JSONB   NOT NULL,
    arena_meldekort_status VARCHAR NOT NULL
)
