package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import arrow.core.Either
import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.domene.FeilVedKorrigeringAvMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.routes.meldekort.logger
import no.nav.tiltakspenger.meldekort.service.KorrigerMeldekortCommand
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import java.time.Clock
import java.time.LocalDate

fun Route.meldekortFraBrukerRoute(
    meldekortService: MeldekortService,
    clock: Clock,
) {
    post("send-inn") {
        val lagreFraBrukerKommando = Either.catch {
            deserialize<MeldekortFraBrukerDTO>(call.receiveText())
                .tilLagreKommando(call.fnr())
        }.getOrElse {
            with("Feil ved parsing av innsendt meldekort fra bruker") {
                logger.error { this }
                Sikkerlogg.error(it) { "$this - ${it.message}" }
            }
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        Either.catch {
            meldekortService.lagreMeldekortFraBruker(kommando = lagreFraBrukerKommando)
        }.onLeft {
            with("Feil ved lagring av innsendt meldekort fra bruker") {
                logger.error { "Feil ved lagring av innsendt meldekort med id ${lagreFraBrukerKommando.id}" }
                Sikkerlogg.error(it) { "$this - ${it.message}" }
            }
            call.respond(HttpStatusCode.InternalServerError)
        }.onRight {
            call.respond(HttpStatusCode.OK)
        }
    }

    data class KorrigertDag(
        val dato: LocalDate,
        val status: MeldekortDagStatus,
    )

    patch("/{meldekortId}/korriger") {
        val meldekortId = MeldekortId.fromString(call.parameters["meldekortId"]!!)
        val korrigerteDagerBody = deserialize<List<KorrigertDag>>(call.receiveText())

        meldekortService.korriger(
            KorrigerMeldekortCommand(
                meldekortId = meldekortId,
                fnr = call.fnr(),
                korrigerteDager = korrigerteDagerBody.map {
                    MeldekortDag(dag = it.dato, status = it.status)
                },
            ),
        ).fold(
            ifLeft = {
                val (status, message) = it.toErrorJson()
                call.respond(status, message)
            },
            ifRight = {
                call.respond(HttpStatusCode.OK, it.tilMeldekortTilBrukerDTO(clock))
            },
        )
    }
}

fun FeilVedKorrigeringAvMeldekort.toErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    FeilVedKorrigeringAvMeldekort.IkkeSisteMeldekortIKjeden -> HttpStatusCode.BadRequest to ErrorJson(
        "Dette meldekortet er allerede korrigert, og er ikke lenger gyldig. Et nyere meldekort finnes.",
        "meldekort_allerede_korrigert_og_ikke_lenger_gyldig",
    )
}
