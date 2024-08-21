package no.nav.tiltakspenger.meldekort.api

import arrow.core.Either
import arrow.core.right
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookup
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupClient
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupFeil
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.meldekort.api.Configuration.httpPort
import no.nav.tiltakspenger.meldekort.api.auth.AzureTokenProvider
import no.nav.tiltakspenger.meldekort.api.clients.dokument.DokumentClient
import no.nav.tiltakspenger.meldekort.api.clients.utbetaling.UtbetalingClient
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.db.flywayMigrate
import no.nav.tiltakspenger.meldekort.api.exceptions.IkkeImplementertException
import no.nav.tiltakspenger.meldekort.api.jobber.GenererMeldekortJobb
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagRepoImpl
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagTiltakRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortDagRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortRepoImpl
import no.nav.tiltakspenger.meldekort.api.repository.UtfallsperiodeDAO
import no.nav.tiltakspenger.meldekort.api.routes.exceptionhandling.ExceptionHandler
import no.nav.tiltakspenger.meldekort.api.routes.healthRoutes
import no.nav.tiltakspenger.meldekort.api.routes.meldekort
import no.nav.tiltakspenger.meldekort.api.service.MeldekortServiceImpl
import no.nav.tiltakspenger.meldekort.api.tilgang.InnloggetBrukerProvider

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())
    val log = KotlinLogging.logger {}

    val securelog = KotlinLogging.logger("tjenestekall")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        securelog.error(e) { e.message }
    }

    log.info { "starter serveren" }

    embeddedServer(Netty, port = httpPort(), module = Application::applicationModule).start(wait = true)
}

fun Application.applicationModule() {
    val log = KotlinLogging.logger {}
    val utbetalingTokenProvider = AzureTokenProvider(config = Configuration.oauthConfigUtbetaling())
    val dokumentTokenProvider = AzureTokenProvider(config = Configuration.oauthConfigDokument())

    val sessionCounter =
        SessionCounter(
            logger = log,
        )
    val dataSource by lazy { DataSource.hikariDataSource }
    val sessionFactory by lazy {
        PostgresSessionFactory(
            dataSource = dataSource,
            sessionCounter = sessionCounter,
        )
    }
    val grunnlagTiltakRepo = GrunnlagTiltakRepo()
    val meldekortDagRepo = MeldekortDagRepo(grunnlagTiltakRepo)
    val meldekortRepo by lazy { MeldekortRepoImpl(meldekortDagRepo, sessionFactory) }
    val utfallsperiodeDAO = UtfallsperiodeDAO()
    val grunnlagRepo by lazy { GrunnlagRepoImpl(grunnlagTiltakRepo, utfallsperiodeDAO, sessionFactory) }
    val utbetalingClient = UtbetalingClient(getToken = utbetalingTokenProvider::getToken)
    val dokumentClient = DokumentClient(getToken = dokumentTokenProvider::getToken)
    val meldekortService by lazy {
        MeldekortServiceImpl(
            meldekortRepo = meldekortRepo,
            meldekortDagRepo = meldekortDagRepo,
            grunnlagRepo = grunnlagRepo,
            grunnlagTiltakRepo = grunnlagTiltakRepo,
            utbetalingClient = utbetalingClient,
            dokumentClient = dokumentClient,
            sessionFactory = sessionFactory,
        )
    }
    val innloggetBrukerProvider = InnloggetBrukerProvider()

    setupKtorExceptionHandling()

    installJacksonFeature()
    flywayMigrate()
    installTokenValidation(Configuration.tokenValidationConfigAzure())
    routing {
        healthRoutes()
        authenticate("azure") {
            meldekort(
                meldekortService,
                innloggetBrukerProvider,
            )
        }
    }

    val runCheckFactory =
        if (Configuration.isNais()) {
            RunCheckFactory(
                leaderPodLookup =
                LeaderPodLookupClient(
                    electorPath = Configuration.electorPath(),
                    logger = KotlinLogging.logger { },
                ),
            )
        } else {
            RunCheckFactory(
                leaderPodLookup =
                object : LeaderPodLookup {
                    override fun amITheLeader(localHostName: String): Either<LeaderPodLookupFeil, Boolean> = true.right()
                },
            )
        }
    GenererMeldekortJobb.startJob(
        runCheckFactory = runCheckFactory,
        meldekortService = meldekortService,
    )
}

fun Application.installJacksonFeature() {
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
        }
    }
}

private fun Application.setupKtorExceptionHandling() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is IkkeImplementertException) {
                call.respondText(text = "Støtter ikke utfall: $cause", status = HttpStatusCode.NotImplemented)
            } else {
                ExceptionHandler.handle(call, cause)
            }
        }
    }
}
