package no.nav.tiltakspenger.meldekort.api.repository

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortUtenDager
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import java.util.UUID

interface MeldekortRepo {
    fun lagre(grunnlagId: UUID, meldekort: Meldekort)

    fun hentMeldekortForGrunnlag(grunnlagId: UUID): List<MeldekortUtenDager>

    fun hentPerioderForMeldekortForGrunnlag(grunnlagId: UUID): List<Periode>

    fun hentMeldekortMedId(meldekortId: UUID): Meldekort?
}
