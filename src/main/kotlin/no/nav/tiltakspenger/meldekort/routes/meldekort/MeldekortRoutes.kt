package no.nav.tiltakspenger.meldekort.routes.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.request.receiveText
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.auth.TexasWall
import no.nav.tiltakspenger.meldekort.auth.fnrAttributeKey
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.domene.genererDummyMeldekort
import no.nav.tiltakspenger.meldekort.service.MeldekortService

val logger = KotlinLogging.logger {}

@Resource("{meldekortId}")
private class HentForMeldekortId(val meldekortId: String)

@Resource("siste")
private class HentSiste()

@Resource("alle")
private class HentAlle()

@Resource("generer")
private class GenererDummyMeldekort()

@Resource("send-inn")
private class SendInn()

internal fun Route.meldekortRoutes(
    meldekortService: MeldekortService,
    texasHttpClient: TexasHttpClient,
) {
    route("/meldekort") {
        install(TexasWall) {
            client = texasHttpClient
        }

        get<HentForMeldekortId> {
            val meldekortId = MeldekortId.Companion.fromString(it.meldekortId)

            val meldekort = meldekortService.hentMeldekort(meldekortId)
            if (meldekort == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(meldekort.tilUtfyllingDTO())
        }

        get<HentSiste> {
            val fnr = Fnr.fromString(call.attributes[fnrAttributeKey])

            val meldekort = meldekortService.hentSisteMeldekort(fnr)
            if (meldekort == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(meldekort.tilUtfyllingDTO())
        }

        get<HentAlle> {
            val fnr = Fnr.fromString(call.attributes[fnrAttributeKey])

            val alleMeldekort = meldekortService.hentAlleMeldekort(fnr).map {
                it.tilUtfyllingDTO()
            }

            call.respond(alleMeldekort)
        }

        get<GenererDummyMeldekort> {
            val fnr = Fnr.fromString(call.attributes[fnrAttributeKey])

            val meldekort = genererDummyMeldekort(fnr)

            meldekortService.lagreMeldekort(meldekort)

            call.respond(meldekort)
        }

        post<SendInn> {
            val body = try {
                deserialize<MeldekortFraUtfyllingDTO>(call.receiveText())
            } catch (e: Exception) {
                logger.error { "Error parsing body: $e" }
                null
            }

            if (body == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            meldekortService.oppdaterMeldekort(body)

            call.respond(HttpStatusCode.OK)
        }
    }
}
