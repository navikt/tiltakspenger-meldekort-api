package no.nav.tiltakspenger.meldekort.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
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
                queryOf(
                    """
                        insert into meldekort (
                            id,
                            sak_id,
                            fnr,
                            fra_og_med,
                            til_og_med,
                            meldeperiode_id,
                            meldekortdager,
                            status,
                            iverksatt_tidspunkt
                        ) values (
                          :id,
                          :sak_id,
                          :fnr,
                          :fra_og_med,
                          :til_og_med,
                          :meldeperiode_id,
                          to_jsonb(:meldekortdager::jsonb),
                          :status,
                          :iverksatt_tidspunkt
                        )
                    """.trimIndent(),
                    mapOf(
                        "id" to meldekort.id.toString(),
                        "sak_id" to meldekort.sakId.toString(),
                        "fnr" to meldekort.fnr.verdi,
                        "fra_og_med" to meldekort.fraOgMed,
                        "til_og_med" to meldekort.tilOgMed,
                        "meldeperiode_id" to meldekort.meldeperiodeId.verdi,
                        "meldekortdager" to serialize(meldekort.meldekortDager),
                        "status" to meldekort.status.name,
                        "iverksatt_tidspunkt" to meldekort.iverksattTidspunkt,
                    ),
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
                queryOf(
                    """
                    update meldekort set 
                        status = :status,
                        meldekortdager = to_jsonb(:meldekortdager::jsonb)
                    where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to meldekort.id,
                        //TODO KEW, ANOM & HEB - Finn ut hvordan status skal settes
                        "status" to "Innsendt",
                        "meldekortdager" to serialize(meldekort.meldekortDager),
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentMeldekort(meldekortId: MeldekortId, transactionContext: TransactionContext?): Meldekort? {
        return sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                queryOf(
                    """
                        select
                          *
                        from meldekort
                        where id = :id
                    """.trimIndent(),
                    mapOf("id" to meldekortId.toString()),
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
        val query = """ 
            select * from meldekort
            where innsendt_tidspunkt is null and meldekortstatus is 'Innsendt'
        """.trimIndent()

        return sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                queryOf(
                    query.trimIndent(),
                ).map { row -> fromRow(row) }.asList,
            )
        }
    }

    override fun markerSendt(meldekortId: MeldekortId, meldekortStatus: MeldekortStatus, innsendtTidspunkt: LocalDateTime, transactionContext: TransactionContext?) {
        val query = """ 
            update meldekort set
            innsendt_tidspunkt = :innsendtTidspunkt,
            status = :meldekortstatus
            where id = :meldekortId
        """.trimIndent()

        return sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                queryOf(
                    query.trimIndent(),
                    mapOf("meldekortstatus" to meldekortStatus.name, "innsendtTidspunkt" to innsendtTidspunkt),
                ).map { row -> fromRow(row) }.asList,
            )
        }
    }

    private fun hentMeldekortForBruker(
        fnr: Fnr,
        limit: Int?,
        transactionContext: TransactionContext?,
    ): List<Meldekort> {
        val query = """
            select
                *
            from meldekort
            where fnr = :fnr
            order by fra_og_med
        """.let { if (limit == null) it else it.plus("limit $limit") }

        return sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                queryOf(
                    query.trimIndent(),
                    mapOf("fnr" to fnr.verdi),
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
                sakId = SakId.fromString(row.string("sak_id")),
                fnr = Fnr.fromString(row.string("fnr")),
                fraOgMed = row.localDate("fra_og_med"),
                tilOgMed = row.localDate("til_og_med"),
                meldeperiodeId = MeldeperiodeId(row.string("meldeperiode_id")),
                meldekortDager = deserializeList(row.string("meldekortdager")),
                status = MeldekortStatus.valueOf(row.string("status")),
                iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt"),
            )
        }
    }
}
