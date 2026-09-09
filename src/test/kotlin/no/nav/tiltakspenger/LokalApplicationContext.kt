package no.nav.tiltakspenger

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.tiltakspenger.fakes.clients.ArenaMeldekortClientFake
import no.nav.tiltakspenger.fakes.clients.DokarkivClientFake
import no.nav.tiltakspenger.fakes.clients.SaksbehandlingClientFake
import no.nav.tiltakspenger.fakes.clients.TexasClientFakeLokal
import no.nav.tiltakspenger.fakes.clients.TmsMikrofrontendClientFake
import no.nav.tiltakspenger.fakes.clients.TmsVarselClientFake
import no.nav.tiltakspenger.generators.JournalpostIdGeneratorRandom
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingClient
import no.nav.tiltakspenger.meldekort.varsler.VarselClient
import java.time.Clock

/**
 * Komposisjonsroten for lokal kjøring, se `LokalMain`.
 * Registeret konstrueres her av samme grunn som i `start()`: alle appens målinger skal føres i det ene registeret `/metrics` skraper.
 */
class LokalApplicationContext(clock: Clock) :
    ApplicationContext(
        clock = clock,
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    ) {
    private val brukFakeTexasClient: Boolean =
        System.getenv("BRUK_FAKE_AUTH")?.toBooleanStrictOrNull() ?: true
    private val brukFakeSaksbehandlingClient: Boolean =
        System.getenv("BRUK_FAKE_SAKSBEHANDLING")?.toBooleanStrictOrNull() ?: false

    override val texasClient =
        if (brukFakeTexasClient) TexasClientFakeLokal(clock) else super.texasClient

    override val varselClient: VarselClient = TmsVarselClientFake()

    override val tmsMikrofrontendClient = TmsMikrofrontendClientFake()

    override val dokarkivClient = DokarkivClientFake(JournalpostIdGeneratorRandom())

    override val saksbehandlingClient: SaksbehandlingClient =
        if (brukFakeSaksbehandlingClient) SaksbehandlingClientFake() else super.saksbehandlingClient

    override val arenaMeldekortClient: ArenaMeldekortClient = ArenaMeldekortClientFake()
}
