package no.nav.tiltakspenger.meldekort.repository

import kotliquery.Row
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.meldekort.domene.Meldekort

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
                            rammevedtak_id,
                            fnr,
                            forrige_meldekort_id,
                            fra_og_med,
                            til_og_med,
                            meldekortdager,
                            status,
                            iverksatt_tidspunkt
                        ) values (
                          :id,
                          :sak_id,
                          :rammevedtak_id,
                          :fnr,
                          :forrige_meldekort_id,
                          :fra_og_med,
                          :til_og_med,
                          to_jsonb(:meldekortdager::jsonb),
                          :status,
                          :iverksatt_tidspunkt
                        )
                    """.trimIndent(),
                    mapOf(
                        "id" to meldekort.id,
                        "sak_id" to meldekort.sakId,
                        "rammevedtak_id" to meldekort.rammevedtakId,
                        "fnr" to meldekort.fnr,
                        "forrige_meldekort_id" to meldekort.forrigeMeldekortId,
                        "fra_og_med" to meldekort.fraOgMed,
                        "til_og_med" to meldekort.tilOgMed,
                        "meldekortdager" to serialize(meldekort.meldekortDager),
                        "status" to meldekort.status,
                        "iverksatt_tidspunkt" to meldekort.iverksattTidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentMeldekort(meldekortId: String, transactionContext: TransactionContext?): Meldekort? {
        return sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                queryOf(
                    """
                        select
                          *
                        from meldekort
                        where id = :id
                    """.trimIndent(),
                    mapOf("id" to meldekortId),
                ).map { row ->
                    fromRow(row)
                }.asSingle,
            )
        }
    }

    override fun hentSisteMeldekort(fnr: String, transactionContext: TransactionContext?): Meldekort? {
        return this.hentMeldekortForBruker(fnr, 1, transactionContext).firstOrNull()
    }

    override fun hentAlleMeldekort(fnr: String, transactionContext: TransactionContext?): List<Meldekort> {
        return this.hentMeldekortForBruker(fnr, null, transactionContext)
    }

    private fun hentMeldekortForBruker(
        fnr: String,
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
                    mapOf("fnr" to fnr),
                ).map { row -> fromRow(row) }.asList,
            )
        }
    }

    companion object {
        private fun fromRow(
            row: Row,
        ): Meldekort {
            return Meldekort(
                id = row.string("id"),
                sakId = row.string("sak_id"),
                rammevedtakId = row.string("rammevedtak_id"),
                fnr = row.string("fnr"),
                fraOgMed = row.localDate("fra_og_med"),
                tilOgMed = row.localDate("til_og_med"),
                meldekortDager = deserializeList(row.string("meldekortdager")),
                status = row.string("status"),
                forrigeMeldekortId = row.stringOrNull("forrige_meldekort_id"),
                iverksattTidspunkt = row.localDateTimeOrNull("iverksatt_tidspunkt"),
            )
        }
    }
}
