package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import java.time.LocalDate
import java.util.*

interface MeldekortService {
    fun genererMeldekort(nyDag: LocalDate)

    fun hentAlleMeldekortene(grunnlagId: UUID): List<Meldekort>

    fun mottaGrunnlag(meldekortGrunnlag: MeldekortGrunnlag)

    fun hentGrunnlagForBehandling(behandlingId: String): MeldekortGrunnlag?

    fun oppdaterMeldekortDag(meldekortId: UUID, tiltakId: UUID, dato: LocalDate, status: MeldekortDagStatus)

    fun hentMeldekort(meldekortId: UUID): Meldekort?

    suspend fun godkjennMeldekort(meldekortId: UUID, saksbehandler: String)
}
