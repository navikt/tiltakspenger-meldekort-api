package no.nav.tiltakspenger.meldekort.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.routes.bearerToken

class AuthPluginConfiguration(
    var client: TexasHttpClient? = null,
)

val fnrAttributeKey = AttributeKey<String>("fnr")

val TexasWall = createRouteScopedPlugin(
    name = "TexasWall",
    createConfiguration = ::AuthPluginConfiguration,
) {
    val log = KotlinLogging.logger("TexasWall")
    val client = pluginConfig.client ?: throw IllegalArgumentException("TexasWall plugin: client must be set")

    pluginConfig.apply {
        onCall { call ->
            val token = call.bearerToken()
            if (token == null) {
                log.warn { "unauthenticated: no Bearer token found in Authorization header" }
                call.respond(HttpStatusCode.Unauthorized)
                return@onCall
            }

            val introspectResponse = try {
                client.introspectToken(token)
            } catch (e: Exception) {
                log.error { "unauthenticated: introspect request failed: ${e.message}" }
                call.respond(HttpStatusCode.Unauthorized)
                return@onCall
            }

            if (introspectResponse.active == false) {
                log.warn { "unauthenticated: ${introspectResponse.error}" }
                call.respond(HttpStatusCode.Unauthorized)
                return@onCall
            }

            log.info { "authenticated - claims='${introspectResponse.other}'" }

            val fnr = introspectResponse.other["pid"]?.toString()
            if (fnr == null) {
                log.warn { "Fant ikke fnr i pid claim" }
                call.respond(HttpStatusCode.InternalServerError)
                return@onCall
            }

            call.attributes.put(fnrAttributeKey, fnr)
        }
    }

    log.info { "NaisAuth plugin loaded." }
}
