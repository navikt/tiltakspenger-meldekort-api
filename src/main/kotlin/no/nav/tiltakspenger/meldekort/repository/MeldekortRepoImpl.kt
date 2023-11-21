package no.nav.tiltakspenger.meldekort.repository

import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO
import javax.sql.DataSource

class MeldekortRepoImpl : MeldekortRepo {
    override fun lagre(id: String, meldekortDto: MeldekortDTO) {
            sessionOf(DataSource.).use {
                it.transaction {txSession ->

                }
            }

    }

}
