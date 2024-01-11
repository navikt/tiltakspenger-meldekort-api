package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

class MeldekortRepoImpl(
    private val meldekortDagRepo: MeldekortDagRepo,
) : MeldekortRepo {
    override fun opprett(grunnlagId: UUID, meldekort: Meldekort.Åpent) {
        sessionOf(DataSource.hikariDataSource).use {
            val id = UUID.randomUUID()
            it.transaction {
                it.run(
                    queryOf(
                        sqlLagreInnsendtMeldekort,
                        mapOf(
                            "id" to id,
                            "grunnlagId" to grunnlagId,
                            "fom" to meldekort.fom,
                            "tom" to meldekort.tom,
                            "type" to "ÅPENT",
                            "sistEndret" to meldekort.sistEndret,
                            "opprettet" to meldekort.opprettet,
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

    override fun hentMeldekort(meldekortId: UUID): Meldekort? {
        return sessionOf(DataSource.hikariDataSource).use { session ->
            session.transaction { txSession ->
                txSession.run(
                    queryOf(
                        sqlHentMeldekort,
                        mapOf(
                            "id" to meldekortId.toString(),
                        ),
                    ).map { row ->
                        row.toMeldekort(txSession)
                    }.asSingle,
                )
            }
        }
    }

    override fun settMeldekortTilInnsendt(meldekort: Meldekort.Innsendt) {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction {
                it.run(
                    queryOf(
                        sqlOppdaterMeldekort,
                        mapOf(
                            "id" to meldekort.id.toString(),
                            "beslutter" to meldekort.beslutter,
                            "sendtInn" to meldekort.sendtInn,
                            "type" to "INNSENDT",
                            "sistEndret" to LocalDate.now(),
                        ),
                    ).asUpdate,
                )
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

    private fun hentForrigeMeldekort(grunnlagId: UUID, fom: LocalDate, tom: LocalDate, txSession: TransactionalSession): Meldekort? {
        return txSession.run(
            queryOf(
                sqlHentMeldekortForPeriode,
                mapOf(
                    "grunnlagId" to grunnlagId.toString(),
                    "fom" to fom,
                    "tom" to tom,
                ),
            ).map { row ->
                row.toMeldekort(txSession)
            }.asSingle,
        )
    }

    private fun Row.toPeriode(): Periode {
        return Periode(
            fra = localDate("fom"),
            til = localDate("tom"),
        )
    }

    private fun Row.toMeldekort(txSession: TransactionalSession): Meldekort {
        val id = UUID.fromString(string("id"))
        val grunnlagId = UUID.fromString(string("grunnlag_id"))
        val fom = localDate("fom")
        val tom = localDate("tom")
        val sistEndret = localDate("sistEndret")
        val opprettet = localDate("opprettet")
        val forrigeMeldekort = hentForrigeMeldekort(grunnlagId, fom, tom, txSession)
        val dager = meldekortDagRepo.hentMeldekortDager(id.toString(), txSession)
        return when (val type = string("type")) {
            "ÅPENT" -> Meldekort.Åpent(
                id = id,
                fom = fom,
                tom = tom,
                meldekortDager = dager,
                forrigeMeldekort = forrigeMeldekort,
                sistEndret = sistEndret,
                opprettet = opprettet,
            )
            "INNSENDT" -> Meldekort.Innsendt(
                id = id,
                fom = fom,
                tom = tom,
                meldekortDager = dager,
                forrigeMeldekort = forrigeMeldekort,
                sistEndret = sistEndret,
                opprettet = opprettet,
                sendtInn = localDate("sendt_inn_dato"),
                beslutter = string("beslutter"),
            )
            else -> throw IllegalArgumentException("Ukjent meldekort type $type")
        }
    }

    @Language("SQL")
    private val sqlLagreInnsendtMeldekort = """
        insert into meldekort (
            id,
            fom,
            tom,
            type,
            grunnlag_id,
            sistEndret,
            opprettet
        ) values (
            :id,
            :fom,
            :tom,
            :type,
            :grunnlagId,
            :sistEndret,
            :opprettet
        )
    """.trimIndent()

    @Language("SQL")
    private val sqlOppdaterMeldekort = """
        update meldekort set
            beslutter = :beslutter,
            sendtInn = :sendtInn,
            type = :type,
            sistEndret = :sistEndret
        where id = :id
    """.trimIndent()

    @Language("SQL")
    private val sqlHentMeldekortForPeriode = """
        select * 
          from meldekort 
         where fom = :fom 
           and tom = :tom
           and grunnlag_id = :grunnlagId
           order by opprettet desc
    """.trimIndent()

    private val sqlHentMeldekortForGrunnlag = """
        select * from meldekort where grunnlag_id = :grunnlagId
    """.trimIndent()

    private val sqlHentMeldekort = """
        select * from meldekort where id = :id
    """.trimIndent()
}
