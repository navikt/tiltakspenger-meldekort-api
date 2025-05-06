package no.nav.tiltakspenger.meldekort.context

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.Profile
import no.nav.tiltakspenger.meldekort.auth.TexasIdentityProvider
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekortServiceClient
import no.nav.tiltakspenger.meldekort.clients.dokarkiv.DokarkivClient
import no.nav.tiltakspenger.meldekort.clients.pdfgen.PdfgenClient
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingClientImpl
import no.nav.tiltakspenger.meldekort.clients.texas.TexasClient
import no.nav.tiltakspenger.meldekort.clients.texas.TexasClientImpl
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClientFake
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClientImpl
import no.nav.tiltakspenger.meldekort.db.DataSourceSetup
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalførMeldekortService
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
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import no.nav.tiltakspenger.meldekort.service.MeldeperiodeService
import no.nav.tiltakspenger.meldekort.service.SakService
import no.nav.tiltakspenger.meldekort.service.SendMeldekortService
import java.time.Clock

@Suppress("unused")
open class ApplicationContext(val clock: Clock) {
    private val log = KotlinLogging.logger {}

    open val jdbcUrl by lazy { Configuration.database() }
    open val dataSource by lazy { DataSourceSetup.createDatasource(jdbcUrl) }
    open val sessionCounter by lazy { SessionCounter(log) }
    open val sessionFactory: SessionFactory by lazy { PostgresSessionFactory(dataSource, sessionCounter) }

    open val texasClient: TexasClient by lazy {
        TexasClientImpl(
            introspectionUrl = Configuration.naisTokenIntrospectionEndpoint,
            tokenUrl = Configuration.naisTokenEndpoint,
            tokenExchangeUrl = Configuration.naisTokenExchangeEndpoint,
        )
    }

    open val tmsVarselClient: TmsVarselClient by lazy {
        if (Configuration.applicationProfile() == Profile.LOCAL) {
            TmsVarselClientFake()
        } else {
            TmsVarselClientImpl(
                kafkaProducer = Producer(KafkaConfigImpl()),
                topicName = Configuration.varselHendelseTopic,
                meldekortFrontendUrl = Configuration.meldekortFrontendUrl,
            )
        }
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
        )
    }

    val meldeperiodeService: MeldeperiodeService by lazy {
        MeldeperiodeService(
            meldeperiodeRepo = meldeperiodeRepo,
            meldekortRepo = meldekortService.meldekortRepo,
            sessionFactory = sessionFactory,
        )
    }

    val sakService: SakService by lazy {
        SakService(
            sakRepo = sakRepo,
            sessionFactory = sessionFactory,
        )
    }

    open val saksbehandlingClient by lazy {
        SaksbehandlingClientImpl(
            baseUrl = Configuration.saksbehandlingApiUrl,
            getToken = {
                texasClient.getSystemToken(
                    Configuration.saksbehandlingApiAudience,
                    TexasIdentityProvider.AZUREAD,
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
                    TexasIdentityProvider.AZUREAD,
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

    open val arenaMeldekortServiceClient by lazy {
        ArenaMeldekortServiceClient(
            texasClient = texasClient,
            baseUrl = Configuration.arenaMeldekortServiceUrl,
            audience = Configuration.arenaMeldekortServiceAudience,
        )
    }
}
