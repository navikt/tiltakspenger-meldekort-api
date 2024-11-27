create table meldekort
(
    id                  varchar primary key,
    sak_id              varchar not null,
    fnr                 varchar not null,
    fra_og_med          date    not null,
    til_og_med          date    not null,
    meldeperiode_id     varchar not null,
    meldekortdager      jsonb   not null,
    status              varchar not null,
    iverksatt_tidspunkt timestamptz null
);