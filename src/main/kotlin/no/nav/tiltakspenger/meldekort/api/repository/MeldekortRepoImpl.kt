package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTOTest
import org.intellij.lang.annotations.Language
import java.util.*

class MeldekortRepoImpl : MeldekortRepo {
    override fun lagre(meldekortDto: MeldekortDTO) {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction {
                it.run(
                    queryOf(
                        sqlLagreMeldekort,
                        mapOf(
                            "id" to UUID.randomUUID(),
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hent(id: String): MeldekortDTOTest? {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction {
                it.run(
                    queryOf(
                        sqlHentMeldekort,
                        mapOf(
                            "id" to id,
                        ),
                    ).map { row ->
                        row.toMeldekortDto()
                    }.asSingle,
                )
            }
        }
    }

    private fun Row.toMeldekortDto(): MeldekortDTOTest {
        return MeldekortDTOTest(
            id = string("id"),
        )
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
    """.trimIndent()

    private val sqlHentMeldekort = """
        select * from meldekort where id = :id
    """.trimIndent()
}
