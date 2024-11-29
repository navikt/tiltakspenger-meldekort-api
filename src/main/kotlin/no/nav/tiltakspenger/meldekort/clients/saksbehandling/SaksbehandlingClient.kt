package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.domene.Meldekort

interface SaksbehandlingClient {
    suspend fun sendMeldekort(meldekort: Meldekort, correlationId: CorrelationId): Either<SaksbehandlingApiError, Unit>
}

data object SaksbehandlingApiError
