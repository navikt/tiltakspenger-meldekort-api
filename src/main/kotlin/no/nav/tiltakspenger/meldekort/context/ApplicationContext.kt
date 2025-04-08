package no.nav.tiltakspenger.meldekort.context

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenClient
import no.nav.tiltakspenger.libs.auth.core.EntraIdSystemtokenHttpClient
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.Profile
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClientImpl
import no.nav.tiltakspenger.meldekort.clients.dokarkiv.DokarkivClient
import no.nav.tiltakspenger.meldekort.clients.pdfgen.PdfgenClient
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingClientImpl
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
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import no.nav.tiltakspenger.meldekort.service.MeldeperiodeService
import no.nav.tiltakspenger.meldekort.service.SendMeldekortService
import java.time.Clock

@Suppress("unused")
open class ApplicationContext(val clock: Clock) {
    private val log = KotlinLogging.logger {}

    open val jdbcUrl by lazy { Configuration.database() }
    open val dataSource by lazy { DataSourceSetup.createDatasource(jdbcUrl) }
    open val sessionCounter by lazy { SessionCounter(log) }
    open val sessionFactory: SessionFactory by lazy { PostgresSessionFactory(dataSource, sessionCounter) }

    open val texasHttpClient: TexasHttpClient by lazy { TexasHttpClientImpl() }

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

    open val saksbehandlingClient by lazy {
        SaksbehandlingClientImpl(
            baseUrl = Configuration.saksbehandlingApiUrl,
            getToken = { texasHttpClient.getSaksbehandlingApiToken() },
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
            getToken = { entraIdSystemtokenClient.getSystemtoken(Configuration.dokarkivScope) },
        )
    }

    open val pdfgenClient by lazy {
        PdfgenClient()
    }

    open val entraIdSystemtokenClient: EntraIdSystemtokenClient by lazy {
        EntraIdSystemtokenHttpClient(
            baseUrl = Configuration.azureOpenidConfigTokenEndpoint,
            clientId = Configuration.azureClientId,
            clientSecret = Configuration.azureClientSecret,
        )
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
}
