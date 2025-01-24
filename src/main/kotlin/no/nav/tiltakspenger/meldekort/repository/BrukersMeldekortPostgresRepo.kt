package no.nav.tiltakspenger.meldekort.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import java.time.LocalDateTime

val logger = KotlinLogging.logger {}

class MeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BrukersMeldekortRepo {

    override fun lagre(meldekort: BrukersMeldekort, sessionContext: SessionContext?) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        insert into meldekort_bruker (
                            id,
                            meldeperiode_id,
                            sak_id,
                            mottatt,
                            dager
                        ) values (
                          :id,
                          :meldeperiode_id,
                          :sak_id,
                          :mottatt,
                          to_jsonb(:dager::jsonb)
                        )
                    """,
                    "id" to meldekort.id.toString(),
                    "meldeperiode_id" to meldekort.meldeperiode.kjedeId,
                    "sak_id" to meldekort.periode.fraOgMed,
                    "mottatt" to meldekort.periode.fraOgMed,
                    "dager" to meldekort.dager.toDbJson(),
                ).asUpdate,
            )
        }
    }

    /** TODO jah: Denne må returnere en liste dersom vi støtter flere innsender på samme meldeperiode */
    override fun hentMeldekortForMeldeperiodeId(
        meldeperiodeId: String,
        sessionContext: SessionContext?,
    ): BrukersMeldekort? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select * from meldekort_bruker where meldeperiode_id = :meldeperiode_id",
                    "meldeperiode_id" to meldeperiodeId,
                ).map { row ->
                    fromRow(row, session)
                }.asSingle,
            )
        }
    }

    /** TODO jah: Denne må returnere en liste dersom vi støtter flere innsender på samme meldeperiode */
    override fun hentMeldekortForMeldeperiodeKjedeId(
        meldeperiodeKjedeId: String,
        sessionContext: SessionContext?,
    ): BrukersMeldekort? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select mk.* from meldekort_bruker mk join meldeperiode mp on mk.meldeperiode_id = mp.id where mp.kjede_id = :kjede_id",
                    "kjede_id" to meldeperiodeKjedeId,
                ).map { row ->
                    fromRow(row, session)
                }.asSingle,
            )
        }
    }

    override fun hentSisteMeldekort(fnr: Fnr, sessionContext: SessionContext?): BrukersMeldekort? {
        return this.hentMeldekortForBruker(fnr, 1, sessionContext).firstOrNull()
    }

    override fun hentAlleMeldekort(fnr: Fnr, sessionContext: SessionContext?): List<BrukersMeldekort> {
        return this.hentMeldekortForBruker(fnr, 100, sessionContext)
    }

    override fun hentUsendteMeldekort(sessionContext: SessionContext?): List<BrukersMeldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """ 
                        select * from meldekort_bruker
                        where innsendt_tidspunkt is null and status = '${MeldekortStatus.INNSENDT.name}'
                    """,
                )
                    .map { row -> fromRow(row, session) }.asList,
            )
        }
    }

    override fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """ 
                        update meldekort_bruker set
                        sendt_til_saksbehandling = :sendtTidspunkt
                        where id = :meldekortId
                    """,
                    "sendtTidspunkt" to sendtTidspunkt,
                ).asUpdate,
            )
        }
    }

    private fun hentMeldekortForBruker(
        fnr: Fnr,
        limit: Int = 100,
        sessionContext: SessionContext?,
    ): List<BrukersMeldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        select
                            *
                        from meldekort_bruker
                        where fnr = :fnr
                        order by fra_og_med desc
                        limit $limit
                    """,
                    "fnr" to fnr.verdi,
                ).map { row -> fromRow(row, session) }.asList,
            )
        }
    }

    companion object {
        private fun fromRow(
            row: Row,
            session: Session,
        ): BrukersMeldekort {
            return BrukersMeldekort(
                id = MeldekortId.Companion.fromString(row.string("id")),
                mottatt = row.localDateTime("mottatt"),
                meldeperiode = MeldeperiodePostgresRepo.hentForId(row.string("meldeperiode_id"), session)!!,
                sakId = SakId.fromString(row.string("sak_id")),
                dager = row.string("dager").toMeldekortDager(),
            )
        }
    }
}
