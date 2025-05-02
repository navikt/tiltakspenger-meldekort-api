package no.nav.tiltakspenger.meldekort.repository

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.Sak

class SakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : SakRepo {
    override fun lagre(
        sak: Sak,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    insert into sak (
                        id,
                        saksnummer,
                        fnr,
                        innvilgelsesperioder,
                        arena_meldekort_status
                    ) values (
                        :id,
                        :saksnummer,
                        :fnr,
                        :innvilgelsesperioder,
                        :arena_meldekort_status                        
                    )
                    """,
                    "id" to sak.id.toString(),
                    "saksnummer" to sak.saksnummer,
                    "fnr" to sak.fnr.verdi,
                    "innvilgelsesperioder" to sak.innvilgelsesperioder.tilDb(),
                    "arena_meldekort_status" to sak.arenaMeldekortStatus.tilDb(),
                ).asUpdate,
            )
        }
    }

    override fun oppdater(
        sak: Sak,
        sessionContext: SessionContext?,
    ) {
        TODO("Not yet implemented")
    }

    override fun hent(
        id: SakId,
        sessionContext: SessionContext?,
    ): Sak? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select * from sak where id = :id",
                    "id" to id.toString(),
                ).map { row -> fromRow(row) }.asSingle,
            )
        }
    }

    companion object {
        private fun fromRow(row: Row): Sak {
            return Sak(
                id = SakId.fromString(row.string("id")),
                saksnummer = row.string("saksnummer"),
                fnr = Fnr.fromString(row.string("fnr")),
                innvilgelsesperioder = row.string("innvilgelsesperioder").tilInnvilgelsesperioder(),
                arenaMeldekortStatus = row.string("arena_meldekort_status").tilArenaMeldekortStatus(),
            )
        }
    }
}
