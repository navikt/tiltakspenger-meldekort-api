package no.nav.tiltakspenger

import no.nav.tiltakspenger.fakes.SakVarselRepoFake
import no.nav.tiltakspenger.fakes.VarselMeldekortRepoFake
import no.nav.tiltakspenger.fakes.VarselRepoFake
import no.nav.tiltakspenger.fakes.clients.TexasClientFakeTest
import no.nav.tiltakspenger.fakes.repos.MeldekortRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldekortvedtakRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldeperiodeRepoFake
import no.nav.tiltakspenger.fakes.repos.SakRepoFake
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.test.common.TestSessionFactory
import java.time.Clock

/**
 * Test-kontekst som bruker in-memory fakes for repositories.
 * Brukes for tester som ikke trenger ekte database.
 */
open class TestApplicationContextMedInMemoryDb(
    clock: Clock = TikkendeKlokke(),
    override val texasClient: TexasClientFakeTest = TexasClientFakeTest(),
    override val sessionFactory: TestSessionFactory = TestSessionFactory(),
) : TestApplicationContext(clock) {
    private val meldekortRepoFake = MeldekortRepoFake(clock)
    override val meldekortRepo = meldekortRepoFake
    override val varselRepo = VarselRepoFake(clock)
    override val meldeperiodeRepo = MeldeperiodeRepoFake()
    override val meldekortvedtakRepo = MeldekortvedtakRepoFake()
    override val sakRepo = SakRepoFake(meldeperiodeRepo, meldekortvedtakRepo)
    override val sakVarselRepo = SakVarselRepoFake(sakRepo)
    override val varselMeldekortRepo = VarselMeldekortRepoFake(meldekortRepoFake, meldeperiodeRepo)
}
