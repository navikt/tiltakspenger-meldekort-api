package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.dto.Meldekort
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortMedTiltak
import java.time.LocalDate
import java.util.*

interface MeldekortService {
    fun genererMeldekort(nyDag: LocalDate)

    fun hentMeldekort(id: String): MeldekortMedTiltak?

    fun hentAlleMeldekortene(grunnlagId: UUID): List<Meldekort>

    fun mottaGrunnlag(meldekortGrunnlag: MeldekortGrunnlag)

    fun hentGrunnlagForBehandling(behandlingId: String): MeldekortGrunnlag?
}
