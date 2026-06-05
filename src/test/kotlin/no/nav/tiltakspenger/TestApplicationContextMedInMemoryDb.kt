package no.nav.tiltakspenger

import no.nav.tiltakspenger.fakes.SakVarselRepoFake
import no.nav.tiltakspenger.fakes.VarselMeldekortRepoFake
import no.nav.tiltakspenger.fakes.VarselRepoFake
import no.nav.tiltakspenger.fakes.clients.TexasClientFakeTest
import no.nav.tiltakspenger.fakes.repos.BrukerSakRepoFake
import no.nav.tiltakspenger.fakes.repos.LandingssideRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldekortRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldekortvedtakRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldeperiodeRepoFake
import no.nav.tiltakspenger.fakes.repos.MottakRepoFake
import no.nav.tiltakspenger.fakes.repos.SakRepoFake
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.test.common.TestSessionFactory
import no.nav.tiltakspenger.meldekort.mottak.MottakRepo
import java.time.Clock

/**
 * Test-kontekst som bruker in-memory fakes for repositories.
 * Brukes for tester som ikke trenger ekte database.
 */
open class TestApplicationContextMedInMemoryDb(
    clock: Clock = TikkendeKlokke(),
    override val sessionFactory: TestSessionFactory = TestSessionFactory(),
) : TestApplicationContext(clock) {
    override val texasClient: TexasClientFakeTest = TexasClientFakeTest()
    private val meldekortvedtakRepoFake = MeldekortvedtakRepoFake()
    override val meldekortvedtakRepo = meldekortvedtakRepoFake
    private val meldekortRepoFake = MeldekortRepoFake(clock, meldekortvedtakRepoFake)
    override val meldekortRepo = meldekortRepoFake
    override val varselRepo = VarselRepoFake(clock)
    private val meldeperiodeRepoFake = MeldeperiodeRepoFake()
    override val meldeperiodeRepo = meldeperiodeRepoFake
    private val sakRepoFake = SakRepoFake(meldeperiodeRepoFake, meldekortvedtakRepoFake)
    override val sakRepo = sakRepoFake
    override val mottakRepo: MottakRepo = MottakRepoFake(sakRepoFake, meldeperiodeRepoFake, meldekortvedtakRepoFake)
    override val brukerSakRepo = BrukerSakRepoFake(sakRepo)
    override val landingssideRepo = LandingssideRepoFake(sakRepo, meldekortRepoFake, meldekortvedtakRepoFake)
    override val sakVarselRepo = SakVarselRepoFake(sakRepo)
    override val varselMeldekortRepo = VarselMeldekortRepoFake(meldekortRepoFake, meldeperiodeRepoFake)
}
