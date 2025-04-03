package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * meldekort-api er master for brukers meldekort.
 * Denne klassen er ansvarlig for å validere.
 *
 * Se også BrukersMeldekort i tiltakspenger-saksbehandling-api.
 *
 * @param id Unik identifikator for denne utfyllingen/innsendingen. Eies av meldekort-api.
 * @param deaktivert settes dersom meldeperioden har fått en ny versjon (pga revurdering), og forrige meldekort-versjon ikke er mottatt fra bruker
 * @param mottatt Tidspunktet mottatt fra bruker
 * @param dager Et innslag per dag i meldeperioden. Må være sortert.
 */
data class Meldekort(
    val id: MeldekortId,
    val deaktivert: LocalDateTime?,
    val mottatt: LocalDateTime?,
    val meldeperiode: Meldeperiode,
    val sakId: SakId,
    val dager: List<MeldekortDag>,
    val journalpostId: JournalpostId?,
    val journalføringstidspunkt: LocalDateTime?,
    val varselId: VarselId? = null,
    val erVarselInaktivert: Boolean = false,
) {
    val periode: Periode = meldeperiode.periode
    val fnr: Fnr = meldeperiode.fnr
    val status: MeldekortStatus = if (mottatt == null) MeldekortStatus.KAN_UTFYLLES else MeldekortStatus.INNSENDT

    init {
        dager.zipWithNext().forEach { (dag, nesteDag) ->
            require(dag.dag.isBefore(nesteDag.dag)) { "Dager må være sortert (id=$id)" }
        }
        require(dager.first().dag == periode.fraOgMed) { "Første dag i meldekortet må være lik første dag i meldeperioden (id=$id)" }
        require(dager.last().dag == periode.tilOgMed) { "Siste dag i meldekortet må være lik siste dag i meldeperioden (id=$id)" }
        require(dager.size.toLong() == periode.antallDager) { "Antall dager i meldekortet må være lik antall dager i meldeperioden (id=$id)" }
        require(meldeperiode.girRett.values.any { it }) { "Meldeperioden for meldekortet må ha minst en dag som gir rett (id=$id)" }
    }

    fun kanSendes() = !periode.tilOgMed.isAfter(senesteTilOgMedDatoForInnsending())
}

fun Meldeperiode.tilTomtMeldekort(): Meldekort {
    return Meldekort(
        id = MeldekortId.random(),
        mottatt = null,
        deaktivert = null,
        meldeperiode = this,
        sakId = this.sakId,
        dager = this.girRett.map {
            MeldekortDag(
                dag = it.key,
                status = MeldekortDagStatus.IKKE_REGISTRERT,
            )
        },
        journalpostId = null,
        journalføringstidspunkt = null,
        varselId = null,
        erVarselInaktivert = false,
    )
}

fun Meldeperiode.tilOppdatertMeldekort(forrigeMeldekort: Meldekort): Meldekort {
    return forrigeMeldekort.copy(
        id = MeldekortId.random(),
        meldeperiode = this,
    )
}

const val DAGER_FØR_PERIODE_SLUTT_FOR_INNSENDING = 2L

fun senesteTilOgMedDatoForInnsending() = LocalDate.now().plusDays(DAGER_FØR_PERIODE_SLUTT_FOR_INNSENDING)
