package no.nav.tiltakspenger

import io.github.oshai.kotlinlogging.KLogger
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
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import no.nav.tiltakspenger.meldekort.microfrontend.AktiverMicrofrontendJob
import no.nav.tiltakspenger.meldekort.microfrontend.InaktiverMicrofrontendJob
import no.nav.tiltakspenger.meldekort.mottak.MottakFraSaksbehandlingService
import java.time.Clock

/**
 * Felles base for test-kontekster.
 * Inneholder fakes for eksterne klienter som er felles for alle test-varianter.
 * Bruk [TestApplicationContextMedInMemoryDb] for tester uten database, eller [TestApplicationContextMedPostgres] for tester med ekte Postgres.
 */
sealed class TestApplicationContext(
    clock: Clock,
    val saksnummergenerator: SaksnummerGeneratorForTest = SaksnummerGeneratorForTest(),
    val fnrGenerator: FnrGenerator = FnrGenerator(),
    journalpostIdGenerator: JournalpostIdGenerator = JournalpostIdGeneratorSerial(),
) : ApplicationContext(clock) {
    /** Fungerer bare for tester som bruker [TikkendeKlokke] som clock */
    val tikkendeKlokke: TikkendeKlokke by lazy { clock as TikkendeKlokke }

    /**
     * Egen fanger per testkontekst, slik at tester kan asserte på logglinjene fra [no.nav.tiltakspenger.meldekort.mottak.MottakFraSaksbehandlingService] uten å dele tilstand med andre tester.
     */
    val mottakLoggfanger = Loggfanger(MottakFraSaksbehandlingService::class.java.name)
    override val mottakFraSaksbehandlingLogger: KLogger = mottakLoggfanger

    /**
     * Fangere for mottaksrutas feilsti, som splitter feil i en peker i vanlig logg og detaljene i sikkerlogg.
     * Kun denne rutas sikkerlogg fanges — den delte [sikkerlogg] står urørt, så resten av appens sikkerlogglinjer er fortsatt synlige under testkjøring.
     */
    val mottakRouteLoggfanger = Loggfanger("no.nav.tiltakspenger.meldekort.mottak.infra.routes.MottakFraSaksbehandlingRouteKt")
    override val mottakFraSaksbehandlingRouteLogger: KLogger = mottakRouteLoggfanger

    val mottakRouteSikkerloggfanger = Sikkerloggfanger()
    override val mottakFraSaksbehandlingRouteSikkerlogg: Sikkerlogg = mottakRouteSikkerloggfanger

    /**
     * Microfrontend-jobbene svelger hentefeil og logger dem, så logglinja er det eneste sporet av at feilhåndteringen kjørte.
     * [no.nav.tiltakspenger.fakes.repos.MicrofrontendRepoFake] returnerer `DatabaseFeil` med vilje for å treffe nettopp den grenen.
     */
    val sendInnMeldekortLoggfanger = Loggfanger("no.nav.tiltakspenger.meldekort.meldekort.infra.routes.SendInnMeldekortRouteKt")
    override val sendInnMeldekortRouteLogger: KLogger = sendInnMeldekortLoggfanger

    val sendInnMeldekortSikkerloggfanger = Sikkerloggfanger()
    override val sendInnMeldekortRouteSikkerlogg: Sikkerlogg = sendInnMeldekortSikkerloggfanger

    val microfrontendRoutesLoggfanger = Loggfanger("no.nav.tiltakspenger.meldekort.microfrontend.infra.routes.MicrofrontendRoutesKt")
    override val microfrontendRoutesLogger: KLogger = microfrontendRoutesLoggfanger

    val aktiverMicrofrontendLoggfanger = Loggfanger(AktiverMicrofrontendJob::class.java.name)
    override val aktiverMicrofrontendJobLogger: KLogger = aktiverMicrofrontendLoggfanger

    val inaktiverMicrofrontendLoggfanger = Loggfanger(InaktiverMicrofrontendJob::class.java.name)
    override val inaktiverMicrofrontendJobLogger: KLogger = inaktiverMicrofrontendLoggfanger

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
