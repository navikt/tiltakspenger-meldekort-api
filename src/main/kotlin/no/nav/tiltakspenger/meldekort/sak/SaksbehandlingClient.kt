package no.nav.tiltakspenger.meldekort.sak

import arrow.core.Either
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort

interface SaksbehandlingClient {
    suspend fun sendMeldekort(meldekort: BrukersMeldekort): Either<SaksbehandlingApiError, Unit>
}

data object SaksbehandlingApiError
