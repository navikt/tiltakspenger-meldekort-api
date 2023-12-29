package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.api.routes.MeldekortGrunnlagDTO

interface MeldekortService {
    suspend fun opprettMeldekort(meldekortDTO: MeldekortDTO)

    suspend fun hentMeldekort(meldekortIdent: String)

    suspend fun hentAlleMeldekortene(sakId: String)

    fun mottaGrunnlag(meldekortGrunnlagDTO: MeldekortGrunnlagDTO)
}
