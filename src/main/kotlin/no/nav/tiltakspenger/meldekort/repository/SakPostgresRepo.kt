package no.nav.tiltakspenger.meldekort.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
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
                        arena_meldekort_status
                    ) values (
                        :id,
                        :saksnummer,
                        :fnr,
                        :arena_meldekort_status                        
                    )
                    """,
                    "id" to sak.id.toString(),
                    "saksnummer" to sak.saksnummer,
                    "fnr" to sak.fnr.verdi,
                    "arena_meldekort_status" to sak.arenaMeldekortStatus.tilDb(),
                ).asUpdate,
            )
        }
    }

    /** Oppdaterer fnr på en sak */
    override fun oppdater(
        sak: Sak,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    update sak set 
                        fnr = :fnr
                    where id = :id
                    """,
                    "id" to sak.id.toString(),
                    "fnr" to sak.fnr.verdi,
                ).asUpdate,
            )
        }
    }

    override fun oppdaterArenaStatus(
        id: SakId,
        arenaStatus: ArenaMeldekortStatus,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    update sak set arena_meldekort_status = :arena_meldekort_status where id = :id
                    """,
                    "id" to id.toString(),
                    "arena_meldekort_status" to arenaStatus.tilDb(),
                ).asUpdate,
            )
        }
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
                ).map { row -> fromRow(row, true, session) }.asSingle,
            )
        }
    }

    override fun hentTilBruker(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Sak? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select * from sak where fnr = :fnr",
                    "fnr" to fnr.verdi,
                ).map { row -> fromRow(row, false, session) }.asSingle,
            )
        }
    }

    override fun hentSakerUtenArenaStatus(sessionContext: SessionContext?): List<Sak> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select * from sak where arena_meldekort_status = :ukjent_status",
                    "ukjent_status" to ArenaMeldekortStatus.UKJENT.tilDb(),
                ).map { row -> fromRow(row, false, session) }.asList,
            )
        }
    }

    companion object {
        private fun fromRow(row: Row, medMeldeperioder: Boolean, session: Session): Sak {
            val sakId = SakId.fromString(row.string("id"))

            val meldeperioder =
                if (medMeldeperioder) MeldeperiodePostgresRepo.hentForSakId(sakId, session) else emptyList()

            return Sak(
                id = sakId,
                saksnummer = row.string("saksnummer"),
                fnr = Fnr.fromString(row.string("fnr")),
                meldeperioder = meldeperioder,
                arenaMeldekortStatus = row.string("arena_meldekort_status").tilArenaMeldekortStatus(),
            )
        }
    }
}
