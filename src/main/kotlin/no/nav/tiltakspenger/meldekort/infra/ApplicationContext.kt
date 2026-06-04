package no.nav.tiltakspenger.meldekort.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
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
import no.nav.tiltakspenger.meldekort.journalføring.PdfgenClient
import no.nav.tiltakspenger.meldekort.journalføring.infra.DokarkivClientImpl
import no.nav.tiltakspenger.meldekort.journalføring.infra.PdfgenClientImpl
import no.nav.tiltakspenger.meldekort.landingsside.FellesLandingssideService
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideRepo
import no.nav.tiltakspenger.meldekort.landingsside.infra.repo.LandingssidePostgresRepo
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortRepo
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortService
import no.nav.tiltakspenger.meldekort.meldekort.SendMeldekortService
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.meldeperiode.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.meldekort.microfrontend.AktiverMicrofrontendJob
import no.nav.tiltakspenger.meldekort.microfrontend.InaktiverMicrofrontendJob
import no.nav.tiltakspenger.meldekort.microfrontend.TmsMikrofrontendClient
import no.nav.tiltakspenger.meldekort.microfrontend.infra.TmsMikrofrontendClientImpl
import no.nav.tiltakspenger.meldekort.mottak.MottakFraSaksbehandlingService
import no.nav.tiltakspenger.meldekort.mottak.MottakRepo
import no.nav.tiltakspenger.meldekort.mottak.infra.MottakPostgresRepo
import no.nav.tiltakspenger.meldekort.sak.SakRepo
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingClient
import no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo
import no.nav.tiltakspenger.meldekort.sak.infra.SaksbehandlingClientImpl
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
        TmsMikrofrontendClientImpl(
            kafkaProducer = Producer(KafkaConfigImpl()),
            topicName = Configuration.microfrontendTopic,
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
    open val meldeperiodeRepo: MeldeperiodeRepo by lazy {
        MeldeperiodePostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    open val sakRepo: SakRepo by lazy {
        SakPostgresRepo(
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

    open val meldekortService: MeldekortService by lazy {
        MeldekortService(
            meldekortRepo = meldekortRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            brukerSakRepo = brukerSakRepo,
            sakVarselRepo = sakVarselRepo,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    open val fellesLandingssideService: FellesLandingssideService by lazy {
        FellesLandingssideService(
            landingssideRepo = landingssideRepo,
            arenaMeldekortClient = arenaMeldekortClient,
            redirectUrl = Configuration.meldekortFrontendUrl,
        )
    }

    open val saksbehandlingClient: SaksbehandlingClient by lazy {
        SaksbehandlingClientImpl(
            baseUrl = Configuration.saksbehandlingApiUrl,
            getToken = {
                texasClient.getSystemToken(
                    Configuration.saksbehandlingApiAudience,
                    IdentityProvider.AZUREAD,
                )
            },
        )
    }

    val sendMeldekortService: SendMeldekortService by lazy {
        SendMeldekortService(
            meldekortService = meldekortService,
            saksbehandlingClient = saksbehandlingClient,
            clock = clock,
        )
    }

    open val journalførMeldekortService: JournalførMeldekortService by lazy {
        JournalførMeldekortService(
            meldekortRepo = meldekortRepo,
            pdfgenClient = pdfgenClient,
            dokarkivClient = dokarkivClient,
            clock = clock,
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
            getToken = {
                texasClient.getSystemToken(
                    Configuration.dokarkivScope,
                    IdentityProvider.AZUREAD,
                )
            },
        )
    }

    open val pdfgenClient: PdfgenClient by lazy {
        PdfgenClientImpl()
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
            getToken = {
                texasClient.getSystemToken(
                    Configuration.arenaMeldekortServiceAudience,
                    IdentityProvider.AZUREAD,
                )
            },
        )
    }

    open val arenaMeldekortStatusService by lazy {
        ArenaMeldekortStatusService(
            arenaMeldekortClient = arenaMeldekortClient,
            sakRepo = sakRepo,
        )
    }

    open val brukerService by lazy {
        BrukerService(
            meldekortService = meldekortService,
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
            sakRepo = sakRepo,
            tmsMikrofrontendClient = tmsMikrofrontendClient,
        )
    }

    open val inaktiverMicrofrontendJob: InaktiverMicrofrontendJob by lazy {
        InaktiverMicrofrontendJob(
            sakRepo = sakRepo,
            tmsMikrofrontendClient = tmsMikrofrontendClient,
        )
    }
}
