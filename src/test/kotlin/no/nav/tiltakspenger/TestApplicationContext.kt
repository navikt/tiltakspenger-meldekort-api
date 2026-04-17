package no.nav.tiltakspenger

import no.nav.tiltakspenger.generators.SaksnummerGeneratorForTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.meldekort.clients.dokarkiv.DokarkivClientFake
import no.nav.tiltakspenger.meldekort.clients.microfrontend.TmsMikrofrontendClientFake
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingClientFake
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClientFake
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import java.time.Clock

/**
 * Felles base for test-kontekster.
 * Inneholder fakes for eksterne klienter som er felles for alle test-varianter.
 * Bruk [TestApplicationContextMedInMemoryDb] for tester uten database,
 * eller [TestApplicationContextMedPostgres] for tester med ekte Postgres.
 */
sealed class TestApplicationContext(clock: Clock) : ApplicationContext(clock) {
    /** Fungerer bare for tester som bruker [TikkendeKlokke] som clock */
    val tikkendeKlokke: TikkendeKlokke by lazy { clock as TikkendeKlokke }
    override val varselClient = TmsVarselClientFake()
    override val tmsMikrofrontendClient = TmsMikrofrontendClientFake()
    override val dokarkivClient = DokarkivClientFake()
    override val saksbehandlingClient = SaksbehandlingClientFake()

    val saksnummergenerator = SaksnummerGeneratorForTest()

    fun nesteSaksnummer(): String = saksnummergenerator.generer()
}
