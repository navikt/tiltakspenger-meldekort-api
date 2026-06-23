package no.nav.tiltakspenger

import no.nav.tiltakspenger.fakes.clients.ArenaMeldekortClientFake
import no.nav.tiltakspenger.fakes.clients.DokarkivClientFake
import no.nav.tiltakspenger.fakes.clients.SaksbehandlingClientFake
import no.nav.tiltakspenger.fakes.clients.TexasClientFakeLokal
import no.nav.tiltakspenger.fakes.clients.TmsMikrofrontendClientFake
import no.nav.tiltakspenger.fakes.clients.TmsVarselClientFake
import no.nav.tiltakspenger.generators.JournalpostIdGeneratorRandom
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import no.nav.tiltakspenger.meldekort.infra.Configuration
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingClient
import no.nav.tiltakspenger.meldekort.varsler.VarselClient
import java.time.Clock

class LokalApplicationContext(clock: Clock) : ApplicationContext(clock) {
    override val texasClient =
        if (Configuration.brukFakeTexasClientLokalt) TexasClientFakeLokal() else super.texasClient

    override val varselClient: VarselClient = TmsVarselClientFake()

    override val tmsMikrofrontendClient = TmsMikrofrontendClientFake()

    override val dokarkivClient = DokarkivClientFake(JournalpostIdGeneratorRandom())

    override val saksbehandlingClient: SaksbehandlingClient =
        if (Configuration.brukFakeSaksbehandlingClient) SaksbehandlingClientFake() else super.saksbehandlingClient

    override val arenaMeldekortClient: ArenaMeldekortClient = ArenaMeldekortClientFake()
}
