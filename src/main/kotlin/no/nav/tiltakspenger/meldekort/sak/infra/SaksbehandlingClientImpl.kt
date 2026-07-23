package no.nav.tiltakspenger.meldekort.sak.infra

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingClient
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient for å sende innsendte meldekort til tiltakspenger-saksbehandling-api.
 *
 * Kildekode: https://github.com/navikt/tiltakspenger-saksbehandling-api
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * Ingen retry, som den gamle klienten; jobben som kaller prøver usendte meldekort på nytt ved neste kjøring.
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class SaksbehandlingClientImpl(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 10.seconds,
    timeout: Duration = 10.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : SaksbehandlingClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    private val saksbehandlingApiMeldekortUri = URI.create("$baseUrl/meldekort/motta")

    override suspend fun sendMeldekort(meldekort: BrukersMeldekort): Either<HttpKlientError, Unit> {
        return httpKlient.postJsonUtenSvar(
            uri = saksbehandlingApiMeldekortUri,
            body = SerialisertJson(serialize(meldekort.toSaksbehandlingMeldekortDTO())),
            godta = Statusregel.Eksakt(200),
        ).map { }
    }
}
