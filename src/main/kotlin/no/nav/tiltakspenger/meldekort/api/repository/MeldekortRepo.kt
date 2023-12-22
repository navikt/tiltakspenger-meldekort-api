package no.nav.tiltakspenger.meldekort.api.repository

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortMedTiltakDTO
import java.util.UUID

interface MeldekortRepo {
    fun lagre(grunnlagId: UUID, meldekort: Meldekort)

    fun hentMeldekortForGrunnlag(grunnlagId: UUID): List<Meldekort>

    fun hentPerioderForMeldekortForGrunnlag(grunnlagId: UUID): List<Periode>

    fun hent(id: String): MeldekortMedTiltakDTO?

    fun hentAlleForIdent(ident: String): List<MeldekortMedTiltakDTO>
}
