package no.nav.tiltakspenger.meldekort.repository

import kotliquery.Row
import kotliquery.queryOf
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
                          :meldekortdager,
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
                        "meldekortdager" to "meldekort.meldekortDager", // TODO Kew: Lag om til Json
                        "status" to meldekort.status,
                        "iverksatt_tidspunkt" to meldekort.iverksattTidspunkt,
                    ),
                ).map { row ->
                    fromRow(row)
                }.asSingle,
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

    companion object {
        private fun fromRow(
            row: Row,
        ): Meldekort {
            return Meldekort(
                id = row.string("id"),
                sakId = row.string("sak_id"),
                rammevedtakId = row.string("rammevedtak_id"),
                fnr = row.string("fnr"),
                forrigeMeldekortId = row.string("forrige_meldekort_id"),
                fraOgMed = row.localDate("fra_og_med"),
                tilOgMed = row.localDate("til_og_med"),
                meldekortDager = row.string("meldekortdager"),
                status = row.string("status"),
                iverksattTidspunkt = row.localDateTime("iverksatt_tidspunkt")
            )
        }
    }
}