package no.nav.tiltakspenger

import no.nav.tiltakspenger.fakes.clients.DokarkivClientFake
import no.nav.tiltakspenger.fakes.clients.SaksbehandlingClientFake
import no.nav.tiltakspenger.fakes.clients.TexasClientFakeLokal
import no.nav.tiltakspenger.fakes.clients.TmsMikrofrontendClientFake
import no.nav.tiltakspenger.fakes.clients.TmsVarselClientFake
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingClient
import no.nav.tiltakspenger.meldekort.clients.varsler.VarselClient
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import java.time.Clock

class LokalApplicationContext(clock: Clock) : ApplicationContext(clock) {
    override val texasClient =
        if (Configuration.brukFakeTexasClientLokalt) TexasClientFakeLokal() else super.texasClient

    override val varselClient: VarselClient = TmsVarselClientFake()

    override val tmsMikrofrontendClient = TmsMikrofrontendClientFake()

    override val dokarkivClient = DokarkivClientFake()

    override val saksbehandlingClient: SaksbehandlingClient =
        if (Configuration.brukFakeSaksbehandlingClient) SaksbehandlingClientFake() else super.saksbehandlingClient
}
