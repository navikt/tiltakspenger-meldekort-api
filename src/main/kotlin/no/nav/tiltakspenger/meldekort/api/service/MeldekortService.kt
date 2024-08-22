package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortUtenDager
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortBeregningDTO
import java.time.LocalDate
import java.util.UUID

interface MeldekortService {
    fun genererMeldekort(nyDag: LocalDate)

    fun hentMeldekort(meldekortId: UUID): Meldekort?

    fun hentAlleMeldekortene(grunnlagId: UUID): List<MeldekortUtenDager>

    fun mottaGrunnlag(meldekortGrunnlag: MeldekortGrunnlag)

    fun hentGrunnlagForBehandling(behandlingId: String): MeldekortGrunnlag?

    fun oppdaterMeldekortDag(meldekortId: UUID, dato: LocalDate, status: MeldekortDagStatus)

    suspend fun hentMeldekortBeregning(meldekortId: UUID): MeldekortBeregningDTO

    suspend fun godkjennMeldekort(meldekortId: UUID, saksbehandler: String)
}
