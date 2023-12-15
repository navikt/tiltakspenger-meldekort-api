package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTOTest
import no.nav.tiltakspenger.meldekort.api.routes.MeldekortGrunnlagDTO
import org.intellij.lang.annotations.Language
import java.util.*

class GrunnlagRepoImpl(
    private val tiltakRepo: GrunnlagTiltakRepo,
) : GrunnlagRepo {
    override fun lagre(dto: MeldekortGrunnlagDTO) {
        sessionOf(DataSource.hikariDataSource).use {
            val id = UUID.randomUUID()
            it.transaction {
                it.run(
                    queryOf(
                        sqlLagreGrunnlag,
                        mapOf(
                            "id" to id.toString(),
                            "behandlingId" to dto.behandlingId,
                            "vedtakId" to dto.vedtakId,
                            "status" to dto.status.name,
                            "fom" to dto.vurderingsperiode.fra,
                            "tom" to dto.vurderingsperiode.til,
                        ),
                    ).asUpdate,
                )
            }.also {
                dto.tiltak.forEach {tiltak ->
                    tiltakRepo.lagre(id.toString(), tiltak)
                }
            }
        }
    }

//    override fun hent(id: String): MeldekortGrunnlagDTO? {
//        return sessionOf(DataSource.hikariDataSource).use {
//            it.transaction {
//                it.run(
//                    queryOf(
//                        sqlHentGrunnlag,
//                        mapOf(
//                            "id" to id,
//                        ),
//                    ).map { row ->
//                        row.toGrunnlagDto()
//                    }.asSingle,
//                )
//            }
//        }
//    }
//
//    private fun Row.toGrunnlagDto(): MeldekortGrunnlagDTO {
//        return MeldekortGrunnlagDTO(
//        )
//    }

//    override fun hentAlleForBehandling(id: String): List<MeldekortGrunnlagDTO> {
//        TODO("Not yet implemented")
//    }

    @Language("SQL")
    private val sqlLagreGrunnlag = """
        insert into grunnlag (
            id,
            behandling_id,
            vedtak_id,
            status,
            fom,
            tom
        ) values (
            :id,
            :behandlingId,
            :vedtakId,
            :status,
            :fom,
            :tom
        )
    """.trimIndent()

//    private val sqlHentGrunnlag = """
//        select * from grunnlag where id = :id
//    """.trimIndent()
//
//    private val sqlHentGrunnlagForBehandling = """
//        select * from grunnlag where behandling_id = :behandlingId
//    """.trimIndent()
}
