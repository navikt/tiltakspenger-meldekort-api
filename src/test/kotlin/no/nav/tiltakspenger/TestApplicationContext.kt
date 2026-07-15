package no.nav.tiltakspenger

import no.nav.tiltakspenger.fakes.clients.ArenaMeldekortClientFake
import no.nav.tiltakspenger.fakes.clients.DokarkivClientFake
import no.nav.tiltakspenger.fakes.clients.SaksbehandlingClientFake
import no.nav.tiltakspenger.fakes.clients.TmsMikrofrontendClientFake
import no.nav.tiltakspenger.fakes.clients.TmsVarselClientFake
import no.nav.tiltakspenger.generators.FnrGenerator
import no.nav.tiltakspenger.generators.JournalpostIdGenerator
import no.nav.tiltakspenger.generators.JournalpostIdGeneratorSerial
import no.nav.tiltakspenger.generators.SaksnummerGeneratorForTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import java.time.Clock

/**
 * Felles base for test-kontekster.
 * Inneholder fakes for eksterne klienter som er felles for alle test-varianter.
 * Bruk [TestApplicationContextMedInMemoryDb] for tester uten database,
 * eller [TestApplicationContextMedPostgres] for tester med ekte Postgres.
 */
sealed class TestApplicationContext(
    clock: Clock,
    val saksnummergenerator: SaksnummerGeneratorForTest = SaksnummerGeneratorForTest(),
    val fnrGenerator: FnrGenerator = FnrGenerator(),
    journalpostIdGenerator: JournalpostIdGenerator = JournalpostIdGeneratorSerial(),
) : ApplicationContext(clock) {
    /** Fungerer bare for tester som bruker [TikkendeKlokke] som clock */
    val tikkendeKlokke: TikkendeKlokke by lazy { clock as TikkendeKlokke }

    override val varselClient = TmsVarselClientFake()
    override val tmsMikrofrontendClient = TmsMikrofrontendClientFake()
    override val dokarkivClient = DokarkivClientFake(journalpostIdGenerator)
    override val saksbehandlingClient = SaksbehandlingClientFake()
    override val arenaMeldekortClient = ArenaMeldekortClientFake()

    fun nesteSaksnummer(): String = saksnummergenerator.generer()

    /**
     *  Deterministisk, unikt fnr.
     *  Foretrekkes fremfor `Fnr.random()` for å unngå flaky kollisjoner.
     */
    fun nesteFnr(): Fnr = fnrGenerator.generer()
}
