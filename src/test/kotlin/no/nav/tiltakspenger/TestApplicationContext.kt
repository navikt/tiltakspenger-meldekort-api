package no.nav.tiltakspenger

import mu.KotlinLogging
import no.nav.tiltakspenger.fakes.MeldekortFake
import no.nav.tiltakspenger.fakes.TexasFake
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo

/**
 * Oppretter en tom ApplicationContext for bruk i tester.
 * Dette vil tilsvare en tom intern database og tomme fakes for eksterne tjenester.
 * Bruk service-funksjoner og hjelpemetoder for å legge til data.
 */
class TestApplicationContext : ApplicationContext() {
    private val log = KotlinLogging.logger {}

    override val texasHttpClient: TexasHttpClient = TexasFake()

    override val meldekortRepo: MeldekortRepo = MeldekortFake()

    // TODO: Dette må kanskje lages. Lag nye fakes om det trengs i tester!
//    val meldekortService: MeldekortService by lazy {
//        MeldekortServiceImpl(meldekortRepo = meldekortRepo)
//    }
//
//    open val saksbehandlingClient by lazy {
//        SaksbehandlingClientImpl(
//            baseUrl = Configuration.saksbehandlingApiUrl,
//            getToken = { texasHttpClient.getSaksbehandlingApiToken() },
//        )
//    }
//
//    val sendMeldekortService: SendMeldekortService by lazy {
//        SendMeldekortService(
//            meldekortService = meldekortService,
//            saksbehandlingClient = saksbehandlingClient,
//        )
//    }
}
