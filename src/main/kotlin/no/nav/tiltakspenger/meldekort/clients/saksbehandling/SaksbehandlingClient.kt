package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort

interface SaksbehandlingClient {
    suspend fun sendMeldekort(meldekort: BrukersMeldekort, correlationId: CorrelationId): Either<SaksbehandlingApiError, Unit>
}

data object SaksbehandlingApiError
