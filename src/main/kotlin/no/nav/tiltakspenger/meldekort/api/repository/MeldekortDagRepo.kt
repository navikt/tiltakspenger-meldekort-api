package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.UUID

class MeldekortDagRepo(
    private val grunnlagTiltakRepo: GrunnlagTiltakRepo,
) {
    fun lagre(
        meldekortId: UUID,
        dto: MeldekortDag,
        tx: TransactionalSession,
    ) {
        tx.run(
            queryOf(
                sqlLagreMeldekortDag,
                mapOf(
                    "id" to UUID.randomUUID().toString(),
                    "meldekortId" to meldekortId.toString(),
                    "tiltakId" to dto.tiltak.id.toString(),
                    "dato" to dto.dato,
                    "status" to dto.status.name,
                ),
            ).asUpdate,
        )
    }

    fun oppdater(
        meldekortId: UUID,
        tiltakId: UUID?,
        dato: LocalDate,
        status: String,
    ) {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        sqlOppdaterMeldekortDag,
                        mapOf(
                            "meldekortId" to meldekortId.toString(),
                            "tiltakId" to tiltakId?.toString(),
                            "dato" to dato,
                            "status" to status,
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    fun hentInnsendteMeldekortDagerForGrunnlag(grunnlagId: UUID): List<MeldekortDag> =
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction { tx ->
                hentInnsendteMeldekortDagerForGrunnlag(grunnlagId, tx)
            }
        }

    fun hentInnsendteMeldekortDagerForGrunnlag(
        grunnlagId: UUID,
        txSession: TransactionalSession,
    ): List<MeldekortDag> =
        txSession.run(
            queryOf(
                sqlHentInnsendteMeldekortDagerForGrunnlagId,
                mapOf(
                    "grunnlagId" to grunnlagId.toString(),
                ),
            ).map { row ->
                row.toMeldekortDag(txSession)
            }.asList,
        )

    fun hentMeldekortDager(
        meldekortId: String,
        txSession: TransactionalSession,
    ): List<MeldekortDag> =
        txSession.run(
            queryOf(
                sqlHentMeldekortDagerForMeldekort,
                mapOf(
                    "meldekortId" to meldekortId,
                ),
            ).map { row ->
                row.toMeldekortDag(txSession)
            }.asList,
        )

    private fun Row.toMeldekortDag(txSession: TransactionalSession): MeldekortDag =
        MeldekortDag(
            dato = localDate("dato"),
            tiltak = string("tiltak_id").let { grunnlagTiltakRepo.hentTiltak(it, txSession)!! },
            status = MeldekortDagStatus.valueOf(string("status")),
            løpenr = int("løpenr"),
            meldekortId = UUID.fromString(string("meldekort_id")),
        )

    @Language("SQL")
    private val sqlHentMeldekortDagerForMeldekort =
        """
        select d.*, m.løpenr 
          from meldekortdag d
          
          inner join meldekort m
            on m.id = d.meldekort_id
          
        where d.meldekort_id = :meldekortId
        """.trimIndent()

    @Language("SQL")
    private val sqlHentInnsendteMeldekortDagerForGrunnlagId =
        """
        select d.*, m.løpenr
        from meldekortdag d
        
        inner join meldekort m
            on m.id = d.meldekort_id
        
        where m.grunnlag_id = :grunnlagId
          and m.type = 'INNSENDT'
        """.trimIndent()

    @Language("SQL")
    private val sqlLagreMeldekortDag =
        """
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

    @Language("SQL")
    private val sqlOppdaterMeldekortDag =
        """
        update meldekortdag set 
            status = :status,
            tiltak_id = :tiltakId
        where meldekort_id = :meldekortId
          and dato = :dato
        """.trimIndent()
}
