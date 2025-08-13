package no.nav.tiltakspenger

import no.nav.tiltakspenger.fakes.MeldekortRepoFake
import no.nav.tiltakspenger.fakes.MeldeperiodeRepoFake
import no.nav.tiltakspenger.fakes.SakRepoFake
import no.nav.tiltakspenger.fakes.TexasClientFake
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClientFake
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import java.time.Clock

/**
 * Oppretter en tom ApplicationContext for bruk i tester.
 * Dette vil tilsvare en tom intern database og tomme fakes for eksterne tjenester.
 * Bruk service-funksjoner og hjelpemetoder for å legge til data.
 */
class TestApplicationContext(clock: Clock = TikkendeKlokke()) : ApplicationContext(clock) {
    override val sessionFactory: TestSessionFactory = TestSessionFactory()

    override val texasClient = TexasClientFake()

    override val tmsVarselClient = TmsVarselClientFake()

    override val meldekortRepo = MeldekortRepoFake(clock)

    override val meldeperiodeRepo = MeldeperiodeRepoFake()

    override val sakRepo = SakRepoFake()
}
