DO
$$
    BEGIN
        IF
            EXISTS
                (SELECT 1 from pg_roles where rolname = 'cloudsqliamuser')
        THEN
            GRANT USAGE ON SCHEMA public TO cloudsqliamuser;
            GRANT
                SELECT
                ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
            ALTER
                DEFAULT PRIVILEGES IN SCHEMA public GRANT
                SELECT
                ON TABLES TO cloudsqliamuser;
        END IF;
    END
$$;

create table grunnlag
(
    id                  varchar primary key,
    sak_id              varchar not null,
    behandling_id       varchar not null,
    vedtak_id           varchar not null,
    status              varchar not null,
    fom                 date    not null,
    tom                 date    not null,
    fornavn             varchar not null,
    etternavn           varchar not null,
    ident               varchar not null
);

create table grunnlag_tiltak
(
    id                  varchar primary key,
    grunnlag_id         varchar references grunnlag(id),
    fom                 date    not null,
    tom                 date    not null,
    typekode            varchar not null,
    typebeskrivelse     varchar not null,
    antall_dager_pr_uke float   not null
);

create table meldekort
(
    id                  varchar primary key,
    grunnlag_id         varchar references grunnlag(id),
    løpenr              int     not null,
    fom                 date    not null,
    tom                 date    not null,
    type                varchar not null,
    saksbehandler       varchar null,
    sendtInn            timestamp null,
    opprettet           timestamp not null,
    sistEndret          timestamp not null,
    journalpostId       varchar null
);

create table meldekortdag
(
    id                  varchar primary key,
    meldekort_id        varchar references meldekort(id),
    tiltak_id           varchar null references grunnlag_tiltak(id),
    dato                date    not null,
    status              varchar not null
);
