package no.nav.tiltakspenger.meldekort.context

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClientImpl
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingClientImpl
import no.nav.tiltakspenger.meldekort.db.DataSourceSetup
import no.nav.tiltakspenger.meldekort.repository.BrukersMeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.BrukersMeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.service.BrukersMeldekortService
import no.nav.tiltakspenger.meldekort.service.MeldeperiodeService
import no.nav.tiltakspenger.meldekort.service.SendMeldekortService

@Suppress("unused")
open class ApplicationContext {
    private val log = KotlinLogging.logger {}

    open val jdbcUrl by lazy { Configuration.database() }
    open val dataSource by lazy { DataSourceSetup.createDatasource(jdbcUrl) }
    open val sessionCounter by lazy { SessionCounter(log) }
    open val sessionFactory: SessionFactory by lazy { PostgresSessionFactory(dataSource, sessionCounter) }

    open val texasHttpClient: TexasHttpClient by lazy { TexasHttpClientImpl() }

    open val brukersMeldekortRepo: BrukersMeldekortRepo by lazy {
        BrukersMeldekortPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    open val meldeperiodeRepo: MeldeperiodeRepo by lazy {
        MeldeperiodePostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }
    val brukersMeldekortService: BrukersMeldekortService by lazy {
        BrukersMeldekortService(brukersMeldekortRepo = brukersMeldekortRepo, meldeperiodeRepo = meldeperiodeRepo)
    }

    val meldeperiodeService: MeldeperiodeService by lazy {
        MeldeperiodeService(repo = meldeperiodeRepo)
    }

    open val saksbehandlingClient by lazy {
        SaksbehandlingClientImpl(
            baseUrl = Configuration.saksbehandlingApiUrl,
            getToken = { texasHttpClient.getSaksbehandlingApiToken() },
        )
    }

    val sendMeldekortService: SendMeldekortService by lazy {
        SendMeldekortService(
            meldekortService = brukersMeldekortService,
            saksbehandlingClient = saksbehandlingClient,
        )
    }
}
