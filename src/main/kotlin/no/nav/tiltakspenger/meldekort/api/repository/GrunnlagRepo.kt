package no.nav.tiltakspenger.meldekort.api.repository

import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTOTest
import no.nav.tiltakspenger.meldekort.api.routes.MeldekortGrunnlagDTO

interface GrunnlagRepo {
    fun lagre(dto: MeldekortGrunnlagDTO)
//    fun hent(id: String): MeldekortGrunnlagDTO?
//    fun hentAlleForBehandling(id: String): List<MeldekortGrunnlagDTO>
}
