package no.nav.tiltakspenger.fakes.clients

import arrow.core.Either
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingApiError
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingClient

class SaksbehandlingClientFake : SaksbehandlingClient {
    override suspend fun sendMeldekort(meldekort: BrukersMeldekort): Either<SaksbehandlingApiError, Unit> {
        return Either.Right(Unit)
    }
}
