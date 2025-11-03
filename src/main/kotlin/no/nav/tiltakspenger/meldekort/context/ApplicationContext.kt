package no.nav.tiltakspenger.meldekort.context

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasHttpClient
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.clients.dokarkiv.DokarkivClient
import no.nav.tiltakspenger.meldekort.clients.microfrontend.TmsMikrofrontendClient
import no.nav.tiltakspenger.meldekort.clients.microfrontend.TmsMikrofrontendClientImpl
import no.nav.tiltakspenger.meldekort.clients.pdfgen.PdfgenClient
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingClientImpl
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClientImpl
import no.nav.tiltakspenger.meldekort.db.DataSourceSetup
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalførMeldekortService
import no.nav.tiltakspenger.meldekort.domene.microfrontend.AktiverMicrofrontendJob
import no.nav.tiltakspenger.meldekort.domene.microfrontend.InaktiverMicrofrontendJob
import no.nav.tiltakspenger.meldekort.domene.varsler.InaktiverVarslerService
import no.nav.tiltakspenger.meldekort.domene.varsler.SendVarslerService
import no.nav.tiltakspenger.meldekort.identhendelse.IdenthendelseConsumer
import no.nav.tiltakspenger.meldekort.identhendelse.IdenthendelseService
import no.nav.tiltakspenger.meldekort.repository.MeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.repository.SakPostgresRepo
import no.nav.tiltakspenger.meldekort.repository.SakRepo
import no.nav.tiltakspenger.meldekort.service.ArenaMeldekortStatusService
import no.nav.tiltakspenger.meldekort.service.BrukerService
import no.nav.tiltakspenger.meldekort.service.LagreFraSaksbehandlingService
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import no.nav.tiltakspenger.meldekort.service.SendMeldekortService
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
        )
    }

    open val tmsVarselClient: TmsVarselClient by lazy {
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
        )
    }

    open val meldekortService: MeldekortService by lazy {
        MeldekortService(
            meldekortRepo = meldekortRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            clock = clock,
        )
    }

    open val saksbehandlingClient by lazy {
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
            meldekortRepo = meldekortRepo,
            tmsVarselClient = tmsVarselClient,
        )
    }

    open val sendVarslerService: SendVarslerService by lazy {
        SendVarslerService(
            meldekortRepo = meldekortRepo,
            tmsVarselClient = tmsVarselClient,
        )
    }

    open val dokarkivClient by lazy {
        DokarkivClient(
            baseUrl = Configuration.dokarkivUrl,
            getToken = {
                texasClient.getSystemToken(
                    Configuration.dokarkivScope,
                    IdentityProvider.AZUREAD,
                )
            },
        )
    }

    open val pdfgenClient by lazy {
        PdfgenClient()
    }

    open val identhendelseService: IdenthendelseService by lazy {
        IdenthendelseService(
            meldeperiodeRepo = meldeperiodeRepo,
        )
    }

    open val identhendelseConsumer by lazy {
        IdenthendelseConsumer(
            identhendelseService = identhendelseService,
            topic = Configuration.identhendelseTopic,
        )
    }

    open val arenaMeldekortClient by lazy {
        ArenaMeldekortClient(
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
            sakRepo = sakRepo,
            arenaMeldekortStatusService = arenaMeldekortStatusService,
        )
    }

    open val lagreFraSaksbehandlingService by lazy {
        LagreFraSaksbehandlingService(
            sakRepo = sakRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            meldekortRepo = meldekortRepo,
            sessionFactory = sessionFactory,
        )
    }

    open val aktiverMicrofrontendJob: AktiverMicrofrontendJob by lazy {
        AktiverMicrofrontendJob(
            sakRepo = sakRepo,
            tmsMikrofrontendClient = tmsMikrofrontendClient,
            clock = clock,
        )
    }

    open val inaktiverMicrofrontendJob: InaktiverMicrofrontendJob by lazy {
        InaktiverMicrofrontendJob(
            sakRepo = sakRepo,
            tmsMikrofrontendClient = tmsMikrofrontendClient,
            clock = clock,
        )
    }
}
