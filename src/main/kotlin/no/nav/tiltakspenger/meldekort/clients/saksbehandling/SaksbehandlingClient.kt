package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import arrow.core.Either
import no.nav.tiltakspenger.meldekort.domene.Meldekort

interface SaksbehandlingClient {
    suspend fun sendMeldekort(meldekort: Meldekort): Either<SaksbehandlingApiError, Unit>
}

data object SaksbehandlingApiError
