package no.nav.tiltakspenger.meldekort.sak

import arrow.core.Either
import no.nav.tiltakspenger.meldekort.meldekort.Meldekort

interface SaksbehandlingClient {
    suspend fun sendMeldekort(meldekort: Meldekort): Either<SaksbehandlingApiError, Unit>
}

data object SaksbehandlingApiError
