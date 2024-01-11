package no.nav.tiltakspenger.meldekort.api.repository

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import java.util.UUID

interface MeldekortRepo {
    fun opprett(grunnlagId: UUID, meldekort: Meldekort.Åpent)

    fun hentMeldekortForGrunnlag(grunnlagId: UUID): List<Meldekort>

    fun hentMeldekort(meldekortId: UUID): Meldekort?

    fun hentPerioderForMeldekortForGrunnlag(grunnlagId: UUID): List<Periode>

    fun settMeldekortTilInnsendt(meldekort: Meldekort.Innsendt)
}
