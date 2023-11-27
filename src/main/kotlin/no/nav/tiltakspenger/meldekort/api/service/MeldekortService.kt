package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTO

interface MeldekortService {
    suspend fun opprettMeldekort(meldekortDTO: MeldekortDTO)

    suspend fun hentMeldekort(meldekortIdent: String)

    suspend fun hentAlleMeldekortene(sakId: String)
}
