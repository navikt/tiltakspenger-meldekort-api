package no.nav.tiltakspenger.meldekort.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.PipelineCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.clients.texas.TexasClient
import no.nav.tiltakspenger.meldekort.clients.texas.TexasIntrospectionResponse
import no.nav.tiltakspenger.meldekort.routes.bearerToken

class AuthPluginConfiguration(
    var client: TexasClient? = null,
)

private val fnrAttributeKey = AttributeKey<Fnr>("fnr")

val log = KotlinLogging.logger("TexasWall")

private suspend fun validateAndGetClaims(
    call: PipelineCall,
    introspectToken: suspend (token: String) -> TexasIntrospectionResponse,
): Map<String, Any?>? {
    val token = call.bearerToken()
    if (token == null) {
        log.warn { "unauthenticated: no Bearer token found in Authorization header" }
        call.respond(HttpStatusCode.Unauthorized)
        return null
    }

    val introspectResponse = try {
        introspectToken(token)
    } catch (e: Exception) {
        log.error { "unauthenticated: introspect request failed: ${e.message}" }
        call.respond(HttpStatusCode.Unauthorized)
        return null
    }

    if (!introspectResponse.active) {
        log.warn { "unauthenticated: ${introspectResponse.error}" }
        call.respond(HttpStatusCode.Unauthorized)
        return null
    }

    return introspectResponse.other
}

val TexasWallBrukerToken = createRouteScopedPlugin(
    name = "TexasWallTokenX",
    createConfiguration = ::AuthPluginConfiguration,
) {
    val client = pluginConfig.client ?: throw IllegalArgumentException("TexasWall plugin: client must be set")

    pluginConfig.apply {
        onCall { call ->
            val tokenClaims =
                validateAndGetClaims(call, { token -> client.introspectToken(token, "tokenx") }) ?: return@onCall

            val fnrString = tokenClaims["pid"]?.toString()
            if (fnrString == null) {
                log.warn { "Fant ikke fnr i pid claim" }
                call.respond(HttpStatusCode.InternalServerError)
                return@onCall
            }

            val fnr = Fnr.fromString(fnrString)

            call.attributes.put(fnrAttributeKey, fnr)
        }
    }
}

val TexasWallSystemToken = createRouteScopedPlugin(
    name = "TexasWallAzure",
    createConfiguration = ::AuthPluginConfiguration,
) {
    val client = pluginConfig.client ?: throw IllegalArgumentException("TexasWall plugin: client must be set")

    pluginConfig.apply {
        onCall { call ->
            validateAndGetClaims(call, { token -> client.introspectToken(token, "azuread") }) ?: return@onCall
        }
    }
}

fun ApplicationCall.fnr(): Fnr {
    return this.attributes[fnrAttributeKey]
}
