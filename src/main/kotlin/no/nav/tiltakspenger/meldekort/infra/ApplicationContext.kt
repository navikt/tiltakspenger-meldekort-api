package no.nav.tiltakspenger.meldekort.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.logging.infra.KotlinLoggingSikkerlogg
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
import no.nav.tiltakspenger.libs.texas.client.TexasSystemTokenProvider
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatusService
import no.nav.tiltakspenger.meldekort.arena.infra.ArenaMeldekortHttpClient
import no.nav.tiltakspenger.meldekort.bruker.BrukerSakRepo
import no.nav.tiltakspenger.meldekort.bruker.BrukerService
import no.nav.tiltakspenger.meldekort.bruker.infra.repo.BrukerSakPostgresRepo
import no.nav.tiltakspenger.meldekort.identhendelse.IdenthendelseService
import no.nav.tiltakspenger.meldekort.identhendelse.infra.IdenthendelseConsumer
import no.nav.tiltakspenger.meldekort.infra.db.DataSourceSetup
import no.nav.tiltakspenger.meldekort.journalføring.DokarkivClient
import no.nav.tiltakspenger.meldekort.journalføring.JournalførMeldekortService
import no.nav.tiltakspenger.meldekort.journalføring.JournalføringRepo
import no.nav.tiltakspenger.meldekort.journalføring.PdfgenClient
import no.nav.tiltakspenger.meldekort.journalføring.infra.DokarkivClientImpl
import no.nav.tiltakspenger.meldekort.journalføring.infra.JournalføringPostgresRepo
import no.nav.tiltakspenger.meldekort.journalføring.infra.PdfgenClientImpl
import no.nav.tiltakspenger.meldekort.landingsside.FellesLandingssideService
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideRepo
import no.nav.tiltakspenger.meldekort.landingsside.infra.repo.LandingssidePostgresRepo
import no.nav.tiltakspenger.meldekort.meldekort.HentMeldekortService
import no.nav.tiltakspenger.meldekort.meldekort.LagreMeldekortFraBrukerService
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortRepo
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.meldekort.korrigering.KorrigerMeldekortService
import no.nav.tiltakspenger.meldekort.meldeperiode.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.meldekort.microfrontend.AktiverMicrofrontendJob
import no.nav.tiltakspenger.meldekort.microfrontend.HentMeldekortInfoForMicrofrontendService
import no.nav.tiltakspenger.meldekort.microfrontend.InaktiverMicrofrontendJob
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendRepo
import no.nav.tiltakspenger.meldekort.microfrontend.TmsMikrofrontendClient
import no.nav.tiltakspenger.meldekort.microfrontend.infra.kafka.TmsMikrofrontendKafkaProducer
import no.nav.tiltakspenger.meldekort.microfrontend.infra.repo.MicrofrontendPostgresRepo
import no.nav.tiltakspenger.meldekort.mottak.MottakFraSaksbehandlingService
import no.nav.tiltakspenger.meldekort.mottak.MottakRepo
import no.nav.tiltakspenger.meldekort.mottak.infra.MottakPostgresRepo
import no.nav.tiltakspenger.meldekort.sak.SakRepo
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingClient
import no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo
import no.nav.tiltakspenger.meldekort.sak.infra.SaksbehandlingClientImpl
import no.nav.tiltakspenger.meldekort.sending.SendMeldekortJobb
import no.nav.tiltakspenger.meldekort.sending.SendMeldekortRepo
import no.nav.tiltakspenger.meldekort.sending.infra.SendMeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.varsler.AktiverVarslerService
import no.nav.tiltakspenger.meldekort.varsler.InaktiverVarslerService
import no.nav.tiltakspenger.meldekort.varsler.SakVarselRepo
import no.nav.tiltakspenger.meldekort.varsler.VarselClient
import no.nav.tiltakspenger.meldekort.varsler.VarselJobber
import no.nav.tiltakspenger.meldekort.varsler.VarselMeldekortRepo
import no.nav.tiltakspenger.meldekort.varsler.VarselRepo
import no.nav.tiltakspenger.meldekort.varsler.VurderVarselService
import no.nav.tiltakspenger.meldekort.varsler.infra.SakVarselPostgresRepo
import no.nav.tiltakspenger.meldekort.varsler.infra.TmsVarselClientImpl
import no.nav.tiltakspenger.meldekort.varsler.infra.VarselMeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.varsler.infra.VarselPostgresRepo
import java.time.Clock

open class ApplicationContext(val clock: Clock) {
    private val log = KotlinLogging.logger {}

    open val jdbcUrl by lazy { Configuration.database() }
    open val dataSource by lazy { DataSourceSetup.createDatasource(jdbcUrl) }
    open val sessionCounter by lazy { SessionCounter(log) }
    open val sessionFactory: SessionFactory by lazy { PostgresSessionFactory(dataSource, sessionCounter) }

    open val texasClient: TexasClient by lazy {
        TexasHttpClient(
            introspectionUrl = Configuration.naisTokenIntrospectionEndpoint,
            tokenUrl = Configuration.naisTokenEndpoint,
            tokenExchangeUrl = Configuration.naisTokenExchangeEndpoint,
            clock = clock,
        )
    }

    open val varselClient: VarselClient by lazy {
        TmsVarselClientImpl(
            kafkaProducer = Producer(KafkaConfigImpl()),
            topicName = Configuration.varselHendelseTopic,
            meldekortFrontendUrl = Configuration.meldekortFrontendUrl,
        )
    }

    open val tmsMikrofrontendClient: TmsMikrofrontendClient by lazy {
        TmsMikrofrontendKafkaProducer(
            topicName = Configuration.microfrontendTopic,
            produserMelding = Producer<String, String>(KafkaConfigImpl())::produce,
        )
    }

    open val varselPostgresRepo by lazy {
        VarselPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            clock = clock,
        )
    }

    open val varselRepo: VarselRepo by lazy { varselPostgresRepo }

    open val meldekortRepo: MeldekortRepo by lazy {
        MeldekortPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            clock = clock,
        )
    }

    open val sendMeldekortRepo: SendMeldekortRepo by lazy {
        SendMeldekortPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    open val journalføringRepo: JournalføringRepo by lazy {
        JournalføringPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    open val meldeperiodeRepo: MeldeperiodeRepo by lazy {
        MeldeperiodePostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    open val sakRepo: SakRepo by lazy {
        SakPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    open val microfrontendRepo: MicrofrontendRepo by lazy {
        MicrofrontendPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            clock = clock,
        )
    }

    open val brukerSakRepo: BrukerSakRepo by lazy {
        BrukerSakPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    open val landingssideRepo: LandingssideRepo by lazy {
        LandingssidePostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            clock = clock,
        )
    }

    open val sakVarselRepo: SakVarselRepo by lazy {
        SakVarselPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    open val mottakRepo: MottakRepo by lazy {
        MottakPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    open val varselMeldekortRepo: VarselMeldekortRepo by lazy {
        VarselMeldekortPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    open val hentMeldekortService: HentMeldekortService by lazy {
        HentMeldekortService(
            meldekortRepo = meldekortRepo,
        )
    }

    open val lagreMeldekortFraBrukerService: LagreMeldekortFraBrukerService by lazy {
        LagreMeldekortFraBrukerService(
            meldekortRepo = meldekortRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            sakVarselRepo = sakVarselRepo,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    open val korrigerMeldekortService: KorrigerMeldekortService by lazy {
        KorrigerMeldekortService(
            meldekortRepo = meldekortRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            brukerSakRepo = brukerSakRepo,
            sakVarselRepo = sakVarselRepo,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    open val hentMeldekortInfoForMicrofrontendService: HentMeldekortInfoForMicrofrontendService by lazy {
        HentMeldekortInfoForMicrofrontendService(
            microfrontendRepo = microfrontendRepo,
        )
    }

    open val fellesLandingssideService: FellesLandingssideService by lazy {
        FellesLandingssideService(
            landingssideRepo = landingssideRepo,
            arenaMeldekortClient = arenaMeldekortClient,
            redirectUrl = Configuration.meldekortFrontendUrl,
            sikkerlogg = sikkerlogg,
        )
    }

    open val sikkerlogg: Sikkerlogg by lazy {
        KotlinLoggingSikkerlogg(
            appNavn = Configuration.naisAppName,
            gcpProsjektId = Configuration.gcpTeamProjectId,
        )
    }

    open val saksbehandlingClient: SaksbehandlingClient by lazy {
        SaksbehandlingClientImpl(
            baseUrl = Configuration.saksbehandlingApiUrl,
            clock = clock,
            authTokenProvider = TexasSystemTokenProvider(
                texasClient = texasClient,
                audienceTarget = Configuration.saksbehandlingApiAudience,
            ),
        )
    }

    val sendMeldekortJobb: SendMeldekortJobb by lazy {
        SendMeldekortJobb(
            sendMeldekortRepo = sendMeldekortRepo,
            saksbehandlingClient = saksbehandlingClient,
            clock = clock,
            sikkerlogg = sikkerlogg,
        )
    }

    open val journalførMeldekortService: JournalførMeldekortService by lazy {
        JournalførMeldekortService(
            journalføringRepo = journalføringRepo,
            pdfgenClient = pdfgenClient,
            dokarkivClient = dokarkivClient,
            clock = clock,
            sikkerlogg = sikkerlogg,
        )
    }

    open val inaktiverVarslerService: InaktiverVarslerService by lazy {
        InaktiverVarslerService(
            varselRepo = varselRepo,
            sakVarselRepo = sakVarselRepo,
            varselClient = varselClient,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    open val aktiverVarslerService: AktiverVarslerService by lazy {
        AktiverVarslerService(
            varselRepo = varselRepo,
            sakVarselRepo = sakVarselRepo,
            varselClient = varselClient,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    open val dokarkivClient: DokarkivClient by lazy {
        DokarkivClientImpl(
            baseUrl = Configuration.dokarkivUrl,
            clock = clock,
            authTokenProvider = TexasSystemTokenProvider(
                texasClient = texasClient,
                audienceTarget = Configuration.dokarkivScope,
            ),
        )
    }

    open val pdfgenClient: PdfgenClient by lazy {
        PdfgenClientImpl(
            isLocalOrDev = !Configuration.isProd(),
            clock = clock,
        )
    }

    open val identhendelseService: IdenthendelseService by lazy {
        IdenthendelseService(
            sakRepo = sakRepo,
        )
    }

    open val identhendelseConsumer by lazy {
        IdenthendelseConsumer(
            identhendelseService = identhendelseService,
            topic = Configuration.identhendelseTopic,
        )
    }

    open val arenaMeldekortClient: ArenaMeldekortClient by lazy {
        ArenaMeldekortHttpClient(
            baseUrl = Configuration.arenaMeldekortServiceUrl,
            clock = clock,
            authTokenProvider = TexasSystemTokenProvider(
                texasClient = texasClient,
                audienceTarget = Configuration.arenaMeldekortServiceAudience,
            ),
        )
    }

    open val arenaMeldekortStatusService by lazy {
        ArenaMeldekortStatusService(
            arenaMeldekortClient = arenaMeldekortClient,
            sakRepo = sakRepo,
            sikkerlogg = sikkerlogg,
        )
    }

    open val brukerService by lazy {
        BrukerService(
            hentMeldekortService = hentMeldekortService,
            brukerSakRepo = brukerSakRepo,
            arenaMeldekortStatusService = arenaMeldekortStatusService,
        )
    }

    open val mottakFraSaksbehandlingService by lazy {
        MottakFraSaksbehandlingService(
            sakRepo = sakRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            meldekortRepo = meldekortRepo,
            sakVarselRepo = sakVarselRepo,
            mottakRepo = mottakRepo,
            sessionFactory = sessionFactory,
        )
    }

    open val vurderVarselService: VurderVarselService by lazy {
        VurderVarselService(
            sakVarselRepo = sakVarselRepo,
            varselMeldekortRepo = varselMeldekortRepo,
            varselRepo = varselRepo,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    open val varselJobber: VarselJobber by lazy {
        VarselJobber(
            vurderVarselService = vurderVarselService,
            aktiverVarslerService = aktiverVarslerService,
            inaktiverVarslerService = inaktiverVarslerService,
        )
    }

    open val aktiverMicrofrontendJob: AktiverMicrofrontendJob by lazy {
        AktiverMicrofrontendJob(
            microfrontendRepo = microfrontendRepo,
            tmsMikrofrontendClient = tmsMikrofrontendClient,
        )
    }

    open val inaktiverMicrofrontendJob: InaktiverMicrofrontendJob by lazy {
        InaktiverMicrofrontendJob(
            microfrontendRepo = microfrontendRepo,
            tmsMikrofrontendClient = tmsMikrofrontendClient,
        )
    }
}
