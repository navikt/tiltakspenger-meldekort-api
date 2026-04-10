package no.nav.tiltakspenger

import no.nav.tiltakspenger.fakes.MeldekortRepoFake
import no.nav.tiltakspenger.fakes.MeldeperiodeRepoFake
import no.nav.tiltakspenger.fakes.SakRepoFake
import no.nav.tiltakspenger.fakes.TexasClientFakeTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.test.common.TestSessionFactory
import java.time.Clock

/**
 * Test-kontekst som bruker in-memory fakes for repositories.
 * Brukes for tester som ikke trenger ekte database.
 */
class TestApplicationContextMedInMemoryDb(
    clock: Clock = TikkendeKlokke(),
    override val texasClient: TexasClientFakeTest = TexasClientFakeTest(),
    override val sessionFactory: TestSessionFactory = TestSessionFactory(),
) : TestApplicationContext(clock) {
    override val meldekortRepo = MeldekortRepoFake(clock)
    override val meldeperiodeRepo = MeldeperiodeRepoFake()
    override val sakRepo = SakRepoFake()
}
