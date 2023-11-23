package no.nav.tiltakspenger.meldekort.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.db.DataSource
import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO
import org.intellij.lang.annotations.Language
import java.util.*
import java.util.UUID

class MeldekortRepoImpl : MeldekortRepo {
    override fun lagre(meldekortDto: MeldekortDTO) {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                it.run(
                    queryOf(
                        sqlLagreMeldekort,
                        mapOf(
                            "id" to UUID.randomUUID(),
                        ),
                    ).asUpdate
                )
            }
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
