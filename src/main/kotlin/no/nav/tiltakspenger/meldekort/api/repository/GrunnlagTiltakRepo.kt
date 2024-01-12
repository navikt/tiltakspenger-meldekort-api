package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.domene.Tiltak
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import org.intellij.lang.annotations.Language
import java.util.*

class GrunnlagTiltakRepo {
    fun lagre(grunnlagId: String, dto: Tiltak) {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction {
                it.run(
                    queryOf(
                        sqlLagreTiltak,
                        mapOf(
                            "id" to dto.id.toString(),
                            "grunnlagId" to grunnlagId,
                            "fom" to dto.periode.fra,
                            "tom" to dto.periode.til,
                            "typeKode" to dto.typeKode,
                            "typeBeskrivelse" to dto.typeBeskrivelse,
                            "antallDagerPrUke" to dto.antallDagerIUken,
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    fun hentTiltakForGrunnlag(grunnlagId: String, txSession: TransactionalSession): List<Tiltak> {
        return txSession.run(
            queryOf(
                sqlHentTiltakForGrunnlag,
                mapOf(
                    "grunnlagId" to grunnlagId,
                ),
            ).map { row ->
                row.toTiltak()
            }.asList,
        )
    }

    fun hentTiltak(tiltakId: String, txSession: TransactionalSession): Tiltak? {
        return txSession.run(
            queryOf(
                sqlHentTiltak,
                mapOf(
                    "id" to tiltakId,
                ),
            ).map { row ->
                row.toTiltak()
            }.asSingle,
        )
    }

    private fun Row.toTiltak(): Tiltak {
        return Tiltak(
            id = UUID.fromString(string("id")),
            periode = Periode(
                fra = localDate("fom"),
                til = localDate("tom"),
            ),
            typeKode = string("typekode"),
            typeBeskrivelse = string("typebeskrivelse"),
            antallDagerIUken = float("antall_dager_pr_uke"),
        )
    }

    @Language("SQL")
    private val sqlHentTiltakForGrunnlag = """
        select * from grunnlag_tiltak where grunnlag_id = :grunnlagId
    """.trimIndent()

    @Language("SQL")
    private val sqlHentTiltak = """
        select * from grunnlag_tiltak where id = :id
    """.trimIndent()

    @Language("SQL")
    private val sqlLagreTiltak = """
        insert into grunnlag_tiltak (
            id,
            grunnlag_id,
            fom,
            tom,
            typekode,
            typebeskrivelse,
            antall_dager_pr_uke
        ) values (
            :id,
            :grunnlagId,
            :fom,
            :tom,
            :typeKode,
            :typeBeskrivelse,
            :antallDagerPrUke
        )
    """.trimIndent()
}
