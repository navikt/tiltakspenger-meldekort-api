package no.nav.tiltakspenger.fakes.clients

import arrow.core.Either
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingApiError
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingClient
import no.nav.tiltakspenger.meldekort.domene.Meldekort

class SaksbehandlingClientFake : SaksbehandlingClient {
    override suspend fun sendMeldekort(meldekort: Meldekort): Either<SaksbehandlingApiError, Unit> {
        return Either.Right(Unit)
    }
}
