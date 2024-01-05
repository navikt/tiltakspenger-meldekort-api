package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import org.intellij.lang.annotations.Language
import java.util.*

class MeldekortRepoImpl(
    private val meldekortDagRepo: MeldekortDagRepo,
) : MeldekortRepo {
    override fun lagre(grunnlagId: UUID, meldekort: Meldekort) {
        sessionOf(DataSource.hikariDataSource).use {
            val id = UUID.randomUUID()
            it.transaction {
                it.run(
                    queryOf(
                        sqlLagreMeldekort,
                        mapOf(
                            "id" to id,
                            "grunnlagId" to grunnlagId,
                            "fom" to meldekort.fom,
                            "tom" to meldekort.tom,
                            "type" to when (meldekort) {
                                is Meldekort.Åpent -> "ÅPENT"
                                is Meldekort.Innsendt -> "INNSENDT"
                            },
                        ),
                    ).asUpdate,
                )
            }.also {
                meldekort.meldekortDager.forEach { dag ->
                    meldekortDagRepo.lagre(id, dag)
                }
            }
        }
    }

    override fun hentPerioderForMeldekortForGrunnlag(grunnlagId: UUID): List<Periode> {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                txSession.run(
                    queryOf(
                        sqlHentMeldekortForGrunnlag,
                        mapOf(
                            "grunnlagId" to grunnlagId.toString(),
                        ),
                    ).map { row ->
                        row.toPeriode()
                    }.asList,
                )
            }
        }
    }

    override fun hentMeldekortForGrunnlag(grunnlagId: UUID): List<Meldekort> {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                txSession.run(
                    queryOf(
                        sqlHentMeldekortForGrunnlag,
                        mapOf(
                            "grunnlagId" to grunnlagId.toString(),
                        ),
                    ).map { row ->
                        row.toMeldekort(txSession)
                    }.asList,
                )
            }
        }
    }

    private fun Row.toPeriode(): Periode {
        return Periode(
            fra = localDate("fom"),
            til = localDate("tom"),
        )
    }

    private fun Row.toMeldekort(txSession: TransactionalSession): Meldekort {
        return when (val type = string("type")) {
            "ÅPENT" -> Meldekort.Åpent(
                id = UUID.fromString(string("id")),
                fom = localDate("fom"),
                tom = localDate("tom"),
                meldekortDager = meldekortDagRepo.hentMeldekortDager(string("id"), txSession),
            )
            "INNSENDT" -> Meldekort.Innsendt(
                id = UUID.fromString(string("id")),
                fom = localDate("fom"),
                tom = localDate("tom"),
                meldekortDager = meldekortDagRepo.hentMeldekortDager(string("id"), txSession),
                sendtInnDato = localDate("sendt_inn_dato"),
            )
            else -> throw IllegalArgumentException("Ukjent meldekort type $type")
        }
    }

    @Language("SQL")
    private val sqlLagreMeldekort = """
        insert into meldekort (
            id,
            fom,
            tom,
            type,
            grunnlag_id
        ) values (
            :id,
            :fom,
            :tom,
            :type,
            :grunnlagId
        )
    """.trimIndent()

    private val sqlHentMeldekortForGrunnlag = """
        select * from meldekort where grunnlag_id = :grunnlagId
    """.trimIndent()

    private val sqlHentMeldekort = """
        select * from meldekort where id = :id
    """.trimIndent()
}
