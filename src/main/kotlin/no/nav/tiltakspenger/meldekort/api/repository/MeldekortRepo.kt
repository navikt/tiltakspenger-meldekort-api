package no.nav.tiltakspenger.meldekort.api.repository

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import java.util.UUID

interface MeldekortRepo {
    fun lagre(grunnlagId: UUID, meldekort: Meldekort)

    fun hentMeldekortForGrunnlag(grunnlagId: UUID): List<Meldekort>

    fun hentPerioderForMeldekortForGrunnlag(grunnlagId: UUID): List<Periode>
}
