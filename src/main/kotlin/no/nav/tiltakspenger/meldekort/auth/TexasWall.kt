package no.nav.tiltakspenger.meldekort.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.response.respondRedirect
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.routes.bearerToken
import no.nav.tiltakspenger.meldekort.routes.loginUrl

class AuthPluginConfiguration(
    var client: TexasHttpClient? = null,
    var ingress: String? = null,
)

val fnrAttributeKey = AttributeKey<String>("fnr")

val TexasWall = createRouteScopedPlugin(
    name = "TexasWall",
    createConfiguration = ::AuthPluginConfiguration,
) {
    val log = KotlinLogging.logger("TexasWall")
    val client = pluginConfig.client ?: throw IllegalArgumentException("TexasWall plugin: client must be set")
    val ingress = pluginConfig.ingress ?: ""

    val challenge: suspend (ApplicationCall) -> Unit = { call ->
        val target = call.loginUrl(ingress)
        log.info { "unauthenticated: redirecting to '$target'" }
        call.respondRedirect(target)
    }

    pluginConfig.apply {
        onCall { call ->
            val token = call.bearerToken()
            if (token == null) {
                log.warn { "unauthenticated: no Bearer token found in Authorization header" }
                challenge(call)
                return@onCall
            }

            val introspectResponse = try {
                client.introspectToken(token)
            } catch (e: Exception) {
                log.error { "unauthenticated: introspect request failed: ${e.message}" }
                challenge(call)
                return@onCall
            }

            if (introspectResponse.active) {
                log.info { "authenticated - claims='${introspectResponse.other}'" }
                call.attributes.put(fnrAttributeKey, introspectResponse.other["pid"].toString())
                return@onCall
            }

            log.warn { "unauthenticated: ${introspectResponse.error}" }
            challenge(call)
            return@onCall
        }
    }

    log.info { "NaisAuth plugin loaded." }
}
