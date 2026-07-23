package no.nav.tiltakspenger.meldekort.sak

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort

interface SaksbehandlingClient {
    suspend fun sendMeldekort(meldekort: BrukersMeldekort): Either<HttpKlientError, Unit>
}
