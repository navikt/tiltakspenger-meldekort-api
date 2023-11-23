package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO

interface MeldekortRepo {
    fun lagre(meldekortDto: MeldekortDTO)

    fun hent(id: String): MeldekortDTO

    fun hentAlle(id: String): List<MeldekortDTO>
}
