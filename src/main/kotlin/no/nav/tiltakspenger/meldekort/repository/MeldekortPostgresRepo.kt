package no.nav.tiltakspenger.meldekort.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.meldekort.db.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortFraUtfyllingDTO
import java.time.LocalDateTime

val logger = KotlinLogging.logger {}

class MeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldekortRepo {
    override fun lagreMeldekort(meldekort: Meldekort, transactionContext: TransactionContext?) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                        insert into meldekort (
                            id,
                            fnr,
                            fra_og_med,
                            til_og_med,
                            meldeperiode_id,
                            meldekortdager,
                            status
                        ) values (
                          :id,
                          :fnr,
                          :fra_og_med,
                          :til_og_med,
                          :meldeperiode_id,
                          to_jsonb(:meldekortdager::jsonb),
                          :status
                        )
                    """,
                    "id" to meldekort.id.toString(),
                    "fnr" to meldekort.fnr.verdi,
                    "fra_og_med" to meldekort.fraOgMed,
                    "til_og_med" to meldekort.tilOgMed,
                    "meldeperiode_id" to meldekort.meldeperiodeId.verdi,
                    "meldekortdager" to serialize(meldekort.meldekortDager),
                    "status" to meldekort.status.name,
                ).asUpdate,
            )
        }
    }

    override fun oppdaterMeldekort(
        meldekort: MeldekortFraUtfyllingDTO,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                        update meldekort set 
                            status = :status,
                            meldekortdager = to_jsonb(:meldekortdager::jsonb)
                        where id = :id
                    """,
                    "id" to meldekort.id,
                    // TODO KEW, ANOM & HEB - Finn ut hvordan status skal settes
                    "status" to MeldekortStatus.INNSENDT.name,
                    "meldekortdager" to serialize(meldekort.meldekortDager),
                ).asUpdate,
            )
        }
    }

    override fun hentMeldekort(meldekortId: MeldekortId, transactionContext: TransactionContext?): Meldekort? {
        return sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                        select
                          *
                        from meldekort
                        where id = :id
                    """,
                    "id" to meldekortId.toString(),
                ).map { row ->
                    fromRow(row)
                }.asSingle,
            )
        }
    }

    override fun hentSisteMeldekort(fnr: Fnr, transactionContext: TransactionContext?): Meldekort? {
        return this.hentMeldekortForBruker(fnr, 1, transactionContext).firstOrNull()
    }

    override fun hentAlleMeldekort(fnr: Fnr, transactionContext: TransactionContext?): List<Meldekort> {
        return this.hentMeldekortForBruker(fnr, null, transactionContext)
    }

    override fun hentUsendteMeldekort(transactionContext: TransactionContext?): List<Meldekort> {
        return sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """ 
                        select * from meldekort
                        where innsendt_tidspunkt is null and status = '${MeldekortStatus.INNSENDT.name}'
                    """,
                )
                    .map { row -> fromRow(row) }.asList,
            )
        }
    }

    override fun markerSendt(
        meldekortId: MeldekortId,
        meldekortStatus: MeldekortStatus,
        innsendtTidspunkt: LocalDateTime,
        transactionContext: TransactionContext?,
    ) {
        return sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """ 
                        update meldekort set
                        innsendt_tidspunkt = :innsendtTidspunkt,
                        status = :meldekortstatus
                        where id = :meldekortId
                    """,
                    "meldekortstatus" to meldekortStatus.name,
                    "innsendtTidspunkt" to innsendtTidspunkt,
                ).asUpdate,
            )
        }
    }

    private fun hentMeldekortForBruker(
        fnr: Fnr,
        limit: Int?,
        transactionContext: TransactionContext?,
    ): List<Meldekort> {
        return sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                        select
                            *
                        from meldekort
                        where fnr = :fnr
                        order by fra_og_med desc
                        limit $limit
                    """,
                    "fnr" to fnr.verdi,
                ).map { row -> fromRow(row) }.asList,
            )
        }
    }

    companion object {
        private fun fromRow(
            row: Row,
        ): Meldekort {
            return Meldekort(
                id = MeldekortId.Companion.fromString(row.string("id")),
                fnr = Fnr.fromString(row.string("fnr")),
                fraOgMed = row.localDate("fra_og_med"),
                tilOgMed = row.localDate("til_og_med"),
                meldeperiodeId = MeldeperiodeId(row.string("meldeperiode_id")),
                meldekortDager = deserializeList(row.string("meldekortdager")),
                status = MeldekortStatus.valueOf(row.string("status")),
            )
        }
    }
}
