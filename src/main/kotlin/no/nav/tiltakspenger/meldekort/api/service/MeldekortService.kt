package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.meldekort.api.dto.Meldekort
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortMedTiltak
import no.nav.tiltakspenger.meldekort.api.routes.MeldekortGrunnlagDTO

interface MeldekortService {
    fun opprettMeldekort(meldekort: Meldekort.Registrert)

    fun hentMeldekort(id: String): MeldekortMedTiltak?

    fun hentAlleMeldekortene(behandlingId: String): List<MeldekortMedTiltak>

    fun mottaGrunnlag(meldekortGrunnlagDTO: MeldekortGrunnlagDTO)
}
