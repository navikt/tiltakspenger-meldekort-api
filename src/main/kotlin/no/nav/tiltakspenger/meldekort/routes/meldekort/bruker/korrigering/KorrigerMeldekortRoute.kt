package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.domene.FeilVedKorrigeringAvMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.service.KorrigerMeldekortCommand
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import java.time.Clock

fun Route.korrigerMeldekortRoute(
    meldekortService: MeldekortService,
    clock: Clock,
) {
    patch("/{meldekortId}/korriger") {
        val meldekortId = MeldekortId.fromString(call.parameters["meldekortId"]!!)
        val korrigerteDagerBody = deserialize<List<MeldekortKorrigertDagDTO>>(call.receiveText())
        val locale = call.request.queryParameters["locale"]

        meldekortService.korriger(
            KorrigerMeldekortCommand(
                meldekortId = meldekortId,
                fnr = call.fnr(),
                korrigerteDager = korrigerteDagerBody.map {
                    MeldekortDag(dag = it.dato, status = it.status)
                },
                locale = locale,
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

    FeilVedKorrigeringAvMeldekort.IngenEndringer -> HttpStatusCode.BadRequest to ErrorJson(
        "Korrigeringen av meldekortet har ingen endringer - Må endre status på minst en dag.",
        "kan_ikke_korrigere_uten_endring",
    )
}
