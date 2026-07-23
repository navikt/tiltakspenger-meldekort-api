package no.nav.tiltakspenger.fakes.clients

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingClient

class SaksbehandlingClientFake : SaksbehandlingClient {
    override suspend fun sendMeldekort(meldekort: BrukersMeldekort): Either<HttpKlientError, Unit> {
        return Unit.right()
    }
}
