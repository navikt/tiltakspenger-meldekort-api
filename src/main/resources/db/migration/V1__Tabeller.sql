create table meldekort
(
    id                   varchar primary key,
    sak_id               varchar not null,
    rammevedtak_id       varchar not null,
    fnr                  varchar not null,
    forrige_meldekort_id varchar null references meldekort(id),
    fra_og_med           date    not null,
    til_og_med           date    not null,
    meldekortdager       jsonb   not null,
    status               varchar not null,
    iverksatt_tidspunkt  timestamptz null
);