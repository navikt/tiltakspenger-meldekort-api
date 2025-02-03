package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDateTime

/**
 * meldekort-api er master for brukers meldekort.
 * Denne klassen er ansvarlig for å validere.
 *
 * Se også BrukersMeldekort i tiltakspenger-saksbehandling-api.
 *
 * @param id Unik identifikator for denne utfyllingen/innsendingen. Eies av meldekort-api.
 * @param mottatt Tidspunktet mottatt fra bruker
 * @param dager Et innslag per dag i meldeperioden. Må være sortert.
 */
data class BrukersMeldekort(
    val id: MeldekortId,
    val mottatt: LocalDateTime?,
    val meldeperiode: Meldeperiode,
    val sakId: SakId,
    val dager: List<BrukersMeldekortDag>,
) {
    val periode: Periode = meldeperiode.periode
    val fnr: Fnr = meldeperiode.fnr

    val status: MeldekortStatus = if (mottatt == null) MeldekortStatus.KAN_UTFYLLES else MeldekortStatus.INNSENDT

    init {
        dager.zipWithNext().forEach { (dag, nesteDag) ->
            require(dag.dag.isBefore(nesteDag.dag)) { "Dager må være sortert" }
        }
        require(dager.first().dag == periode.fraOgMed) { "Første dag i meldekortet må være lik første dag i meldeperioden" }
        require(dager.last().dag == periode.tilOgMed) { "Siste dag i meldekortet må være lik siste dag i meldeperioden" }
        require(dager.size.toLong() == periode.antallDager) { "Antall dager i meldekortet må være lik antall dager i meldeperioden" }
        require(dager.any { it.status != MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER }) { "Brukers meldekort må ha minst en dag som kan utfylles" }
    }
}

fun Meldeperiode.tilTomtBrukersMeldekort(): BrukersMeldekort {
    return BrukersMeldekort(
        id = MeldekortId.random(),
        mottatt = null,
        meldeperiode = this,
        sakId = this.sakId,
        dager = this.girRett.map {
            BrukersMeldekortDag(
                dag = it.key,
                status = if (it.value) MeldekortDagStatus.IKKE_REGISTRERT else MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
            )
        },
    )
}
