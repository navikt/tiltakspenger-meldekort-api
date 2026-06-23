package no.nav.tiltakspenger.dev

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import no.nav.tiltakspenger.meldekort.mottak.infra.tilMottattSak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Dev-only endepunkter. Registreres KUN fra LokalMain (via `additionalRoutes`),
 * og er aldri med i prod-bygget (ligger i test-sourceset og kobles ikke inn fra ktorSetup i prod).
 *
 * Hensikten er å enkelt opprette en sak med meldeperioder lokalt, med fornuftige defaults og noen overrides.
 * Vi gjenbruker testbuilderne ([ObjectMother]) og kjører gjennom akkurat samme mapping ([tilMottattSak])
 * og service ([ApplicationContext.mottakFraSaksbehandlingService]) som det ekte motta-endepunktet
 * (`POST /saksbehandling/sak`) bruker — så flyten blir mest mulig prodlik. Eneste forskjell er at vi hopper
 * over HTTP-auth (AzureAD) og bygger DTO-en selv i stedet for å motta den fra saksbehandling-api.
 */
fun Routing.devRoutes(applicationContext: ApplicationContext) {
    val logger = KotlinLogging.logger {}

    route("/dev") {
        // Oppretter en sak med meldeperioder. Body er valgfri – uten body får du sane defaults.
        post("/sak") {
            val request = call.receiveText()
                .takeIf { it.isNotBlank() }
                ?.let { deserialize<OpprettSakRequest>(it) }
                ?: OpprettSakRequest()

            val clock = applicationContext.clock

            val fnr = request.fnr?.let { Fnr.fromString(it) } ?: Fnr.random()
            val sakId = request.sakId?.let { SakId.fromString(it) } ?: SakId.random()
            val saksnummer = request.saksnummer ?: nesteDevSaksnummer()

            val meldeperioder = byggMeldeperioder(
                antallBakover = request.antallMeldeperioderBakover,
                antallFremover = request.antallMeldeperioderFremover,
                forsteMeldeperiodeStart = request.forsteMeldeperiodeStart,
                opprettet = nå(clock),
                idag = nå(clock).toLocalDate(),
            )

            val sakDTO: SakTilMeldekortApiDTO = ObjectMother.sakDTO(
                sakId = sakId.toString(),
                saksnummer = saksnummer,
                fnr = fnr.verdi,
                meldeperioder = meldeperioder,
                harSoknadUnderBehandling = request.harSoknadUnderBehandling,
                kanSendeInnHelgForMeldekort = request.kanSendeInnHelgForMeldekort,
            )

            // Samme flyt som det ekte motta-endepunktet.
            applicationContext.mottakFraSaksbehandlingService.lagre(sakDTO.tilMottattSak())
                .onLeft {
                    logger.warn { "Dev: klarte ikke lagre sak $sakId: $it" }
                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = serialize(mapOf("feil" to it.toString(), "sakId" to sakId.toString())),
                    )
                }
                .onRight {
                    logger.info { "Dev: opprettet sak $sakId for $fnr med ${meldeperioder.size} meldeperiode(r)" }
                    call.respond(
                        status = HttpStatusCode.Created,
                        message = serialize(
                            OpprettSakResponse(
                                sakId = sakId.toString(),
                                saksnummer = saksnummer,
                                fnr = fnr.verdi,
                                meldeperioder = meldeperioder.map { mp ->
                                    OpprettSakResponse.MeldeperiodeSummary(
                                        kjedeId = mp.kjedeId,
                                        fraOgMed = mp.periodeDTO.fraOgMed,
                                        tilOgMed = mp.periodeDTO.tilOgMed,
                                    )
                                },
                            ),
                        ),
                    )
                }
        }
    }
}

/**
 * Bygger sammenhengende 14-dagers meldeperioder fordelt rundt [idag]: [antallBakover] perioder som ligger
 * helt i fortiden, etterfulgt av [antallFremover] perioder fra og med inneværende periode og fremover.
 * Default (2 + 2) gir altså 2 ferdige meldekort man kan fylle ut nå, og 2 som ligger frem i tid.
 *
 * Settes [forsteMeldeperiodeStart] eksplisitt, ignoreres fordelingen og periodene genereres bare
 * sekvensielt fra og med mandagen på/før den datoen.
 *
 * Gjenbruker [ObjectMother.periode] (mandags-snapping) og `Periode.plus14Dager` (neste sammenhengende
 * periode) i stedet for å regne ut datoene selv.
 */
private fun byggMeldeperioder(
    antallBakover: Int,
    antallFremover: Int,
    forsteMeldeperiodeStart: LocalDate?,
    opprettet: LocalDateTime,
    idag: LocalDate,
): List<SakTilMeldekortApiDTO.MeldeperiodeDTO> {
    val bakover = antallBakover.coerceAtLeast(0)
    val totalt = (bakover + antallFremover.coerceAtLeast(0)).coerceAtLeast(1)

    val forstePeriode = ObjectMother.periode(
        fraSisteMandagFør = forsteMeldeperiodeStart ?: idag.minusWeeks((bakover * 2).toLong()),
    )

    return generateSequence(forstePeriode) { it.plus14Dager() }
        .take(totalt)
        .map { ObjectMother.meldeperiodeDto(periode = it, opprettet = opprettet) }
        .toList()
}

private val devSaksnummerTeller = java.util.concurrent.atomic.AtomicLong(1)

private fun nesteDevSaksnummer(): String = "DEV-${devSaksnummerTeller.getAndIncrement()}"

/**
 * Alle felter er valgfrie – tom body gir sane defaults.
 */
private data class OpprettSakRequest(
    val fnr: String? = null,
    val sakId: String? = null,
    val saksnummer: String? = null,
    val antallMeldeperioderBakover: Int = 2,
    val antallMeldeperioderFremover: Int = 2,
    val forsteMeldeperiodeStart: LocalDate? = null,
    val harSoknadUnderBehandling: Boolean = false,
    val kanSendeInnHelgForMeldekort: Boolean = false,
)

private data class OpprettSakResponse(
    val sakId: String,
    val saksnummer: String,
    val fnr: String,
    val meldeperioder: List<MeldeperiodeSummary>,
) {
    data class MeldeperiodeSummary(
        val kjedeId: String,
        val fraOgMed: String,
        val tilOgMed: String,
    )
}
