package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortUtenDager
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
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
                            "lopenr" to meldekort.løpenr,
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

    override fun lagreInnsendtMeldekort(meldekort: Meldekort.Innsendt, tx: TransactionalSession) {
        tx.run(
            queryOf(
                sqlOppdaterMeldekort,
                mapOf(
                    "id" to meldekort.id.toString(),
                    "saksbehandler" to meldekort.saksbehandler,
                    "sendtInn" to meldekort.sendtInn,
                    "type" to "INNSENDT",
                    "sistEndret" to LocalDateTime.now(),
                ),
            ).asUpdate,
        )
    }

    override fun lagreInnsendtMeldekort(meldekort: Meldekort.Innsendt) {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction { tx ->
                lagreInnsendtMeldekort(meldekort, tx)
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

    override fun hentMeldekortMedId(meldekortId: UUID): Meldekort? {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                txSession.run(
                    queryOf(
                        sqlHentMeldekort,
                        mapOf(
                            "meldekortId" to meldekortId.toString(),
                        ),
                    ).map { row ->
                        row.toMeldekort(txSession)
                    }.asSingle,
                )
            }
        }
    }

    override fun hentMeldekortForGrunnlag(grunnlagId: UUID): List<MeldekortUtenDager> {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                txSession.run(
                    queryOf(
                        sqlHentMeldekortForGrunnlag,
                        mapOf(
                            "grunnlagId" to grunnlagId.toString(),
                        ),
                    ).map { row ->
                        row.toMeldekortUtenDager()
                    }.asList,
                )
            }
        }
    }

    override fun hentGrunnlagIdForMeldekort(meldekortId: UUID): UUID? {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction {
                it.run(
                    queryOf(
                        sqlHentGrunnlagForMeldekort,
                        mapOf(
                            "meldekortId" to meldekortId.toString(),
                        ),
                    ).map { row ->
                        UUID.fromString(row.string("grunnlag_id"))
                    }.asSingle,
                )
            }
        }
    }

    private fun hentForrigeMeldekort(grunnlagId: UUID, løpenr: Int, txSession: TransactionalSession): Meldekort? {
        val forrigeLøpenr = løpenr - 1
        return txSession.run(
            queryOf(
                sqlHentMeldekortForPeriode,
                mapOf(
                    "grunnlagId" to grunnlagId.toString(),
                    "lopenr" to forrigeLøpenr,
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
        val løpenr = int("løpenr")
        val fom = localDate("fom")
        val tom = localDate("tom")
        val sistEndret = localDateTime("sistEndret")
        val opprettet = localDateTime("opprettet")
        val forrigeMeldekort = if (løpenr > 1) hentForrigeMeldekort(grunnlagId, løpenr, txSession) else null
        val dager = meldekortDagRepo.hentMeldekortDager(id.toString(), txSession).sortedBy { it.dato }
        return when (val type = string("type")) {
            "ÅPENT" -> Meldekort.Åpent(
                id = id,
                løpenr = løpenr,
                fom = fom,
                tom = tom,
                meldekortDager = dager,
                forrigeMeldekort = forrigeMeldekort,
                sistEndret = sistEndret,
                opprettet = opprettet,
            )
            "INNSENDT" -> Meldekort.Innsendt(
                id = id,
                løpenr = løpenr,
                fom = fom,
                tom = tom,
                meldekortDager = dager,
                forrigeMeldekort = forrigeMeldekort,
                sistEndret = sistEndret,
                opprettet = opprettet,
                sendtInn = localDateTime("sendtInn"),
                saksbehandler = string("saksbehandler"),
            )
            else -> throw IllegalArgumentException("Ukjent meldekort type $type")
        }
    }

    private fun Row.toMeldekortUtenDager(): MeldekortUtenDager {
        return when (val type = string("type")) {
            "ÅPENT" -> MeldekortUtenDager(
                id = UUID.fromString(string("id")),
                fom = localDate("fom"),
                tom = localDate("tom"),
                status = MeldekortStatus.valueOf("ÅPENT"),
            )
            "INNSENDT" -> MeldekortUtenDager(
                id = UUID.fromString(string("id")),
                fom = localDate("fom"),
                tom = localDate("tom"),
                status = MeldekortStatus.valueOf("INNSENDT"),
            )
            else -> throw IllegalArgumentException("Ukjent meldekort type $type")
        }
    }

    @Language("SQL")
    private val sqlLagreInnsendtMeldekort = """
        insert into meldekort (
            id,
            løpenr,
            fom,
            tom,
            type,
            grunnlag_id,
            sistEndret,
            opprettet
        ) values (
            :id,
            :lopenr,
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
            saksbehandler = :saksbehandler,
            sendtInn = :sendtInn,
            type = :type,
            sistEndret = :sistEndret
        where id = :id
    """.trimIndent()

    @Language("SQL")
    private val sqlHentMeldekortForPeriode = """
        select * 
          from meldekort 
         where grunnlag_id = :grunnlagId 
           and løpenr = :lopenr
           order by opprettet desc
           limit 1
    """.trimIndent()

    private val sqlHentMeldekortForGrunnlag = """
        select * from meldekort where grunnlag_id = :grunnlagId ORDER BY tom DESC
    """.trimIndent()

    private val sqlHentMeldekort = """
        select * from meldekort where id = :meldekortId
    """.trimIndent()

    private val sqlHentGrunnlagForMeldekort = """
        select grunnlag_id from meldekort where id = :meldekortId
    """.trimIndent()
}
