package no.nav.tiltakspenger.routes

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder

suspend fun ApplicationTestBuilder.defaultRequestWithAssertions(
    method: HttpMethod,
    uri: String,
    jwt: String? = JwtGenerator().createJwtForSaksbehandler(),
    forventetStatus: HttpStatusCode,
    forventetBody: String?,
    forventetContentType: ContentType?,
    setup: HttpRequestBuilder.() -> Unit = {},
): HttpResponse {
    val response = defaultRequest(method, uri, jwt, setup)
    val bodyAsText = response.bodyAsText()
    val contentType = response.contentType()
    val status = response.status
    withClue(
        "Response details:\n" +
            "Status: $status\n" +
            "Content-Type: $contentType\n" +
            "Body: $bodyAsText",
    ) {
        if (forventetBody == "") {
            contentType shouldBe null
        }
        if (contentType == null) {
            bodyAsText shouldBe ""
        }
        status shouldBe forventetStatus
        if (forventetBody != null) {
            bodyAsText shouldBe forventetBody
        }
        contentType shouldBe forventetContentType
    }
    return response
}

private suspend fun ApplicationTestBuilder.defaultRequest(
    method: HttpMethod,
    uri: String,
    jwt: String? = JwtGenerator().createJwtForSaksbehandler(),
    setup: HttpRequestBuilder.() -> Unit = {},
): HttpResponse {
    return this.client.request(uri) {
        this.method = method
        this.headers {
            append(HttpHeaders.XCorrelationId, "DEFAULT_CALL_ID")
            append(HttpHeaders.ContentType, ContentType.Application.Json)
            if (jwt != null) append(HttpHeaders.Authorization, "Bearer $jwt")
        }
        setup()
    }
}
