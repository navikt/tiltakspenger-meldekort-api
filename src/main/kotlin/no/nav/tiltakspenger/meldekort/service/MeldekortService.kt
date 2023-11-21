package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO

interface MeldekortService {
    suspend fun leggTilMeldekort(id: String, meldekortDTO: MeldekortDTO)
}
