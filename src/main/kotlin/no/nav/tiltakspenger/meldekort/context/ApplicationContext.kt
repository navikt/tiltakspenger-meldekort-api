package no.nav.tiltakspenger.meldekort.context

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClientImpl
import no.nav.tiltakspenger.meldekort.db.DataSourceSetup
import no.nav.tiltakspenger.meldekort.repository.MeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import no.nav.tiltakspenger.meldekort.service.MeldekortServiceImpl

@Suppress("unused")
open class ApplicationContext() {
    private val log = KotlinLogging.logger {}

    open val jdbcUrl by lazy { Configuration.database() }
    open val dataSource by lazy { DataSourceSetup.createDatasource(jdbcUrl) }
    open val sessionCounter by lazy { SessionCounter(log) }
    open val sessionFactory: SessionFactory by lazy { PostgresSessionFactory(dataSource, sessionCounter) }

    open val texasHttpClient: TexasHttpClient by lazy { TexasHttpClientImpl() }

    open val meldekortRepo: MeldekortRepo by lazy {
        MeldekortPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    val meldekortService: MeldekortService by lazy {
        MeldekortServiceImpl(meldekortRepo = meldekortRepo)
    }
}
