package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.meldekort.KunneIkkeLagreMeldekortFraBruker
import no.nav.tiltakspenger.meldekort.meldekort.LagreMeldekortFraBrukerService
import no.nav.tiltakspenger.meldekort.meldekort.Loggnivå
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortFraBrukerDTO

private val logger = KotlinLogging.logger {}

/**
 * Request DTO: [MeldekortFraBrukerDTO]
 */
fun Route.sendInnMeldekortRoute(
    meldekortService: LagreMeldekortFraBrukerService,
) {
    post("send-inn") {
        val lagreFraBrukerKommando = Either.catch {
            deserialize<MeldekortFraBrukerDTO>(call.receiveText())
                .tilLagreKommando(call.fnr())
        }.getOrElse {
            with("Feil ved parsing av innsendt meldekort fra bruker") {
                logger.error { "$this. Se sikkerlogg for detaljer." }
                Sikkerlogg.error(it) { "$this - ${it.message}" }
            }
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ErrorJson(
                    melding = "Vi klarte ikke å lese inn meldekortet du sendte. Prøv igjen.",
                    kode = "ugyldig_meldekort_innsending",
                ),
            )
            return@post
        }

        meldekortService.lagreMeldekortFraBruker(kommando = lagreFraBrukerKommando)
            .onLeft { feil ->
                feil.logg()
                val (status, errorJson) = feil.toErrorJson()
                call.respond(status = status, message = errorJson)
            }.onRight {
                call.respond(HttpStatusCode.OK)
            }
    }
}

/**
 * Logger feilen i tråd med dens egen tolkning: [KunneIkkeLagreMeldekortFraBruker.loggnivå] avgjør nivået i
 * vanlig logg, mens en eventuell [KunneIkkeLagreMeldekortFraBruker.throwable] (som kan inneholde
 * personopplysninger) kun går til sikkerlogg, med en referanse fra vanlig logg.
 */
private fun KunneIkkeLagreMeldekortFraBruker.logg() {
    val vanligLoggMelding = if (throwable != null) "$loggMelding Se sikkerlogg for detaljer." else loggMelding
    when (loggnivå) {
        Loggnivå.WARN -> logger.warn { vanligLoggMelding }
        Loggnivå.ERROR -> logger.error { vanligLoggMelding }
    }
    throwable?.let { Sikkerlogg.error(it) { "$loggMelding - ${it.message}" } }
}

/**
 * Oversetter en [KunneIkkeLagreMeldekortFraBruker] til en HTTP-status og en [ErrorJson] som bruker kan lese,
 * enten i frontend eller direkte i JSON-responsen. [ErrorJson.melding] er derfor brukervennlig og fri for
 * interne IDer og personopplysninger - de hører kun hjemme i (sikker)loggen via [feil]'ens `loggMelding`.
 */
private fun KunneIkkeLagreMeldekortFraBruker.toErrorJson(): Pair<HttpStatusCode, ErrorJson> = when (this) {
    is KunneIkkeLagreMeldekortFraBruker.FantIkkeMeldekort -> HttpStatusCode.NotFound to ErrorJson(
        melding = "Vi fant ikke meldekortet du prøver å sende inn.",
        kode = "fant_ikke_meldekort",
    )

    is KunneIkkeLagreMeldekortFraBruker.FantIkkeMeldeperiode -> HttpStatusCode.InternalServerError to ErrorJson(
        melding = "Det oppstod en feil med meldekortet ditt. Ta kontakt med oss hvis problemet vedvarer.",
        kode = "fant_ikke_meldeperiode",
    )

    is KunneIkkeLagreMeldekortFraBruker.MeldekortErAlleredeMottatt -> HttpStatusCode.Conflict to ErrorJson(
        melding = "Dette meldekortet er allerede sendt inn.",
        kode = "meldekort_allerede_mottatt",
    )

    is KunneIkkeLagreMeldekortFraBruker.MeldekortErDeaktivert -> HttpStatusCode.Conflict to ErrorJson(
        melding = "Dette meldekortet er ikke lenger gyldig fordi det har kommet en nyere versjon av meldeperioden. " +
            "Gå tilbake til oversikten og send inn det nyeste meldekortet.",
        kode = "meldekort_deaktivert",
    )

    is KunneIkkeLagreMeldekortFraBruker.MeldekortetsMeldeperiodeErErstattet -> HttpStatusCode.Conflict to ErrorJson(
        melding = "Dette meldekortet er ikke lenger gyldig fordi det har kommet en nyere versjon av meldeperioden. " +
            "Gå tilbake til oversikten og send inn det nyeste meldekortet.",
        kode = "meldekortets_meldeperiode_er_erstattet",
    )

    is KunneIkkeLagreMeldekortFraBruker.MeldekortErIkkeKlartTilInnsending -> HttpStatusCode.Conflict to ErrorJson(
        melding = "Dette meldekortet er ikke klart til innsending ennå. Prøv igjen senere.",
        kode = "meldekort_ikke_klart_til_innsending",
    )

    is KunneIkkeLagreMeldekortFraBruker.UventetFeilVedLagring -> HttpStatusCode.InternalServerError to ErrorJson(
        melding = "Det oppstod en uventet feil ved innsending av meldekortet. Prøv igjen senere.",
        kode = "uventet_feil_ved_lagring",
    )
}
