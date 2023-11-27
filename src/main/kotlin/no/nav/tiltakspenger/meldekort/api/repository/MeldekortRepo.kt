package no.nav.tiltakspenger.meldekort.api.repository

import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTOTest

interface MeldekortRepo {
    fun lagre(meldekortDto: MeldekortDTO)

    fun hent(id: String): MeldekortDTOTest?

    fun hentAlle(id: String): List<MeldekortDTO>
}
