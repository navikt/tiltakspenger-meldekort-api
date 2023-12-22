package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDagStatus
import org.intellij.lang.annotations.Language
import java.util.*

class MeldekortDagRepo(
    private val grunnlagTiltakRepo: GrunnlagTiltakRepo,
) {
    fun lagre(meldekortId: UUID, dto: MeldekortDag) {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction {
                it.run(
                    queryOf(
                        sqlLagreMeldekortDag,
                        mapOf(
                            "id" to UUID.randomUUID().toString(),
                            "meldekortId" to meldekortId.toString(),
                            "tiltakId" to if (dto.tiltak == null) null else dto.tiltak.id.toString(),
                            "dato" to dto.dato,
                            "status" to dto.status.name,
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    fun hentMeldekortDager(meldekortId: String, txSession: TransactionalSession): List<MeldekortDag> {
        return txSession.run(
            queryOf(
                sqlHentMeldekortDagerForMeldekort,
                mapOf(
                    "meldekortId" to meldekortId,
                ),
            ).map { row ->
                row.toMeldekortDag(txSession)
            }.asList,
        )
    }

    private fun Row.toMeldekortDag(txSession: TransactionalSession): MeldekortDag {
        return MeldekortDag(
            dato = localDate("dato"),
            tiltak = stringOrNull("tiltak_id")?.let { grunnlagTiltakRepo.hentTiltak(it, txSession) },
            status = MeldekortDagStatus.valueOf(string("status")),
        )
    }

    @Language("SQL")
    private val sqlHentMeldekortDagerForMeldekort = """
        select * from meldekortdag where meldekort_id = :meldekortId
    """.trimIndent()

    @Language("SQL")
    private val sqlLagreMeldekortDag = """
        insert into meldekortdag (
            id,
            meldekort_id,
            tiltak_id,
            dato,
            status
        ) values (
            :id,
            :meldekortId,
            :tiltakId,
            :dato,
            :status
        )
    """.trimIndent()
}
