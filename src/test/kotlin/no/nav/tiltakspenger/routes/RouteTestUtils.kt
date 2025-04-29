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

fun testMedMeldekortRoutes(context: TestApplicationContext, block: suspend ApplicationTestBuilder.() -> Unit) {
    runTest {
        testApplication {
            application {
                jacksonSerialization()
                routing {
                    meldekortRoutes(
                        meldekortService = context.meldekortService,
                        meldeperiodeService = context.meldeperiodeService,
                        texasClient = context.texasClient,
                        clock = context.clock,
                        arenaMeldekortApiClient = context.arenaMeldekortApiClient,
                    )
                }
            }

            block()
        }
    }
}
