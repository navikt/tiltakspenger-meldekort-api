package no.nav.tiltakspenger.meldekort.api.repository

import no.nav.tiltakspenger.meldekort.api.dto.Meldekort
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortMedTiltak
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import java.util.UUID

interface MeldekortRepo {
    fun lagre(grunnlagId: UUID, meldekort: Meldekort)

    fun hentMeldekortForGrunnlag(grunnlagId: UUID): List<Meldekort>

    fun hentPerioderForMeldekortForGrunnlag(grunnlagId: UUID): List<Periode>

    fun hent(id: String): MeldekortMedTiltak?

    fun hentAlle(ident: String): List<MeldekortMedTiltak>
}
