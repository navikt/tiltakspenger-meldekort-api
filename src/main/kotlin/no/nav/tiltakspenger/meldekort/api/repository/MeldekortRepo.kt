package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.dto.MeldekortDTOTest

interface MeldekortRepo {
    fun lagre(meldekortDto: MeldekortDTO)

    fun hent(id: String): MeldekortDTOTest?

    fun hentAlle(id: String): List<MeldekortDTO>
}
