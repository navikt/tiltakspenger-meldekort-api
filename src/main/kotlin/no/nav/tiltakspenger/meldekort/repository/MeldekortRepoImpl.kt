package no.nav.tiltakspenger.meldekort.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.db.DataSource
import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO
import org.intellij.lang.annotations.Language

internal class MeldekortRepoImpl() : MeldekortRepo {
    override fun lagre(id: String, meldekortDto: MeldekortDTO) {
            sessionOf(DataSource.hikariDataSource).use {
                it.run(
                    queryOf(sqlLagreMeldekort,
                        mapOf(
                            "id" to id,
                            "fom" to meldekortDto.fom,
                            "tom" to meldekortDto.tom,
                            "sendtInnDato" to meldekortDto.sendtInnDato
                        ),
                    ).asUpdate
                )
            }
    }

    override fun hent(id: String): MeldekortDTO {
        TODO("Not yet implemented")
    }

    override fun hentAlle(id: String): List<MeldekortDTO> {
        TODO("Not yet implemented")
    }

    @Language("SQL")
    private val sqlLagreMeldekort = """
    insert into meldekort (
        id,
    ) values (
        :id,
    )
    """.trimIndent();

}
