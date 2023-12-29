package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.routes.TiltakDTO
import org.intellij.lang.annotations.Language
import java.util.*

class GrunnlagTiltakRepo {
    fun lagre(grunnlagId: String, dto: TiltakDTO) {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction {
                it.run(
                    queryOf(
                        sqlLagreTiltak,
                        mapOf(
                            "id" to UUID.randomUUID().toString(),
                            "grunnlagId" to grunnlagId,
                            "fom" to dto.periodeDTO.fra,
                            "tom" to dto.periodeDTO.til,
                            "typeKode" to dto.typeKode,
                            "typeBeskrivelse" to dto.typeBeskrivelse,
                            "antallDagerPrUke" to dto.antDagerIUken,
                        ),
                    ).asUpdate,
                )
            }
        }
    }

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
