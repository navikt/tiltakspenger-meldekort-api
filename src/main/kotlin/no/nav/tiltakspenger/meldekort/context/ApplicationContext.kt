package no.nav.tiltakspenger.meldekort.context

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.db.DataSourceSetup

@Suppress("unused")
open class ApplicationContext() {
    private val log = KotlinLogging.logger {}

    open val jdbcUrl by lazy { Configuration.database() }
    open val dataSource by lazy { DataSourceSetup.createDatasource(jdbcUrl) }
}