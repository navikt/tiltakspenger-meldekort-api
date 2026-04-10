package no.nav.tiltakspenger.routes

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.append
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.meldekort.routes.jacksonSerialization
import no.nav.tiltakspenger.meldekort.routes.meldekort.meldekortRoutes
import no.nav.tiltakspenger.meldekort.routes.setupAuthentication

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

inline fun testMedMeldekortRoutes(
    context: TestApplicationContext,
    crossinline block: suspend ApplicationTestBuilder.() -> Unit,
) {
    val callSite = Exception("Call site")
    callSite.stackTrace = callSite.stackTrace
        .filterNot {
            it.className.startsWith("kotlin.coroutines") ||
                it.className.startsWith("kotlinx.coroutines") ||
                it.className.contains("\$\$")
        }
        .toTypedArray()

    runTest {
        testApplication {
            application {
                jacksonSerialization()
                setupAuthentication(context.texasClient)
                routing {
                    meldekortRoutes(
                        meldekortService = context.meldekortService,
                        lagreFraSaksbehandlingService = context.lagreFraSaksbehandlingService,
                        brukerService = context.brukerService,
                        clock = context.clock,
                    )
                }
            }

            try {
                block()
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
}
