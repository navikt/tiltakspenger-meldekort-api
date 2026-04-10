package no.nav.tiltakspenger.routes

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.append
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.TestApplicationContextMedInMemoryDb
import no.nav.tiltakspenger.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.db.TestDatabaseManager
import no.nav.tiltakspenger.fakes.TexasClientFakeTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.test.common.TestSessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import no.nav.tiltakspenger.meldekort.routes.meldekortApi
import java.time.Clock

suspend fun ApplicationTestBuilder.defaultRequest(
    method: HttpMethod,
    uri: String,
    jwt: String? = JwtGenerator().createJwtForSaksbehandler(),
    setup: HttpRequestBuilder.() -> Unit = {},
): HttpResponse =
    this.client.request(uri) {
        this.method = method
        this.headers {
            append(HttpHeaders.XCorrelationId, "DEFAULT_CALL_ID")
            append(HttpHeaders.ContentType, ContentType.Application.Json)
            if (jwt != null) append(HttpHeaders.Authorization, "Bearer $jwt")
        }
        setup()
    }

private val dbManager = TestDatabaseManager()

fun withTestApplicationContextAndPostgres(
    additionalConfig: Application.() -> Unit = {},
    clock: TikkendeKlokke = TikkendeKlokke(fixedClockAt(1.mai(2025))),
    texasClient: TexasClient = TexasClientFakeTest(),
    runIsolated: Boolean = false,
    testBlock: suspend ApplicationTestBuilder.(TestApplicationContextMedPostgres) -> Unit,
) {
    val callSite = captureCallSite()
    runTest {
        dbManager.withMigratedDb(
            runIsolated = runIsolated,
            clock = clock,
        ) { sessionFactory: SessionFactory, _: Clock ->
            val context = TestApplicationContextMedPostgres(
                clock = clock,
                texasClient = texasClient,
                sessionFactory = sessionFactory as PostgresSessionFactory,
            )
            runTestApplication(context, additionalConfig, callSite, testBlock)
        }
    }
}

/**
 * @param clock Merk at vi ikke kan behandle et meldekort før vi har passert meldekortets første dag.Derfor er det viktig at [clock] er satt til en dato etter den første meldekortdagen i testene som bruker denne eller bruk TikkendeKlokke.spolTil(...)
 */
fun withTestApplicationContext(
    additionalConfig: Application.() -> Unit = {},
    clock: TikkendeKlokke = TikkendeKlokke(fixedClockAt(1.mai(2025))),
    texasClient: TexasClientFakeTest = TexasClientFakeTest(),
    sessionFactory: TestSessionFactory = TestSessionFactory(),
    testBlock: suspend ApplicationTestBuilder.(TestApplicationContextMedInMemoryDb) -> Unit,
) {
    val callSite = captureCallSite()
    runTest {
        val context = TestApplicationContextMedInMemoryDb(
            clock = clock,
            texasClient = texasClient,
            sessionFactory = sessionFactory,
        )
        runTestApplication(context, additionalConfig, callSite, testBlock)
    }
}

private fun captureCallSite(): Exception {
    val callSite = Exception("Call site")
    callSite.stackTrace = callSite.stackTrace
        .filterNot {
            it.className.startsWith("kotlin.coroutines") ||
                it.className.startsWith("kotlinx.coroutines") ||
                it.className.contains("\$\$")
        }
        .toTypedArray()
    return callSite
}

private fun <T : ApplicationContext> runTestApplication(
    context: T,
    additionalConfig: Application.() -> Unit,
    callSite: Exception,
    testBlock: suspend ApplicationTestBuilder.(T) -> Unit,
) {
    testApplication {
        application {
            meldekortApi(context)
            additionalConfig()
        }
        try {
            testBlock(context)
        } catch (e: AssertionError) {
            try {
                e.initCause(callSite)
            } catch (_: IllegalStateException) {
                e.addSuppressed(callSite)
            }
            throw e
        }
    }
}
