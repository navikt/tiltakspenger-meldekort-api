package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortMedTiltak
import java.time.LocalDate

interface MeldekortService {
    fun genererMeldekort(fraDato: LocalDate)

    fun hentMeldekort(id: String): MeldekortMedTiltak?

    fun hentAlleMeldekortene(behandlingId: String): List<MeldekortMedTiltak>

    fun mottaGrunnlag(meldekortGrunnlag: MeldekortGrunnlag)
}
