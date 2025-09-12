@file:Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")

package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import java.time.Clock
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
    val dager: List<MeldekortDag>,
    val journalpostId: JournalpostId?,
    val journalføringstidspunkt: LocalDateTime?,
    val varselId: VarselId? = null,
    val erVarselInaktivert: Boolean = false,
) {
    val sakId = meldeperiode.sakId
    val periode: Periode = meldeperiode.periode
    val fnr: Fnr = meldeperiode.fnr
    val saksnummer: String = meldeperiode.saksnummer

    val status: MeldekortStatus = when {
        mottatt != null -> MeldekortStatus.INNSENDT
        deaktivert != null -> MeldekortStatus.DEAKTIVERT
        !periode.tilOgMed.isAfter(senesteTilOgMedDatoForInnsending()) -> MeldekortStatus.KAN_UTFYLLES
        else -> MeldekortStatus.IKKE_KLAR
    }

    /**
     * Om den er klar til innsending nå.
     * OBS! Denne dupliserer logikken som finnes for [no.nav.tiltakspenger.meldekort.repository.MeldekortPostgresRepo.hentAlleMeldekortKlarTilInnsending]. Om den ene endres så burde begge endres
     */
    val klarTilInnsending: Boolean by lazy { status == MeldekortStatus.KAN_UTFYLLES }

    /** Hvilken dag de er klar til innsending eller null dersom den aldri kan sendes inn. */
    val klarTilInnsendingDag: LocalDate? = when (status) {
        MeldekortStatus.KAN_UTFYLLES,
        MeldekortStatus.IKKE_KLAR,
        -> periode.tilOgMed.minusDays(DAGER_FØR_PERIODE_SLUTT_FOR_INNSENDING)

        MeldekortStatus.INNSENDT,
        MeldekortStatus.DEAKTIVERT,
        -> null
    }

    val maksAntallDagerForPeriode = meldeperiode.maksAntallDagerForPeriode

    /** Regner ikke med IKKE_BESVART, IKKE_RETT_TIL_TILTAKSPENGER eller IKKE_TILTAKSDAG som besvart. */
    val antallDagerBesvart = dager.count { it.erBesvart }

    fun fyllUtMeldekortFraBruker(
        sisteMeldeperiode: Meldeperiode,
        clock: Clock,
        brukerutfylteDager: List<MeldekortDag>,
    ): Meldekort {
        if (this.mottatt != null) {
            throw IllegalArgumentException("Meldekort med id ${this.id} er allerede mottatt ($mottatt)")
        }
        return this.copy(
            meldeperiode = sisteMeldeperiode,
            mottatt = LocalDateTime.now(clock),
            dager = brukerutfylteDager,
        )
    }

    fun fyllUtMeldekortFraBruker(
        mottatt: LocalDateTime,
        brukerutfylteDager: List<MeldekortDag>,
    ): Meldekort {
        if (this.mottatt != null) {
            throw IllegalArgumentException("Meldekort med id ${this.id} er allerede mottatt ($mottatt)")
        }
        return this.copy(
            mottatt = mottatt,
            dager = brukerutfylteDager,
        )
    }

    fun inaktiverVarsel(): Meldekort {
        return this.copy(
            erVarselInaktivert = true,
        )
    }

    fun oppdaterVarselId(
        nyttVarselId: VarselId,
    ): Meldekort {
        return this.copy(
            varselId = nyttVarselId,
        )
    }

    init {
        dager.zipWithNext().forEach { (dag, nesteDag) ->
            require(dag.dag.isBefore(nesteDag.dag)) { "Dager må være sortert (id=$id)" }
        }
        require(dager.first().dag == periode.fraOgMed) { "Første dag i meldekortet må være lik første dag i meldeperioden (id=$id)" }
        require(dager.last().dag == periode.tilOgMed) { "Siste dag i meldekortet må være lik siste dag i meldeperioden (id=$id)" }
        require(dager.size.toLong() == periode.antallDager) { "Antall dager i meldekortet må være lik antall dager i meldeperioden (id=$id)" }
        require(meldeperiode.girRett.values.any { it }) { "Meldeperioden for meldekortet må ha minst en dag som gir rett (id=$id)" }
        meldeperiode.girRett.values.zip(dager.map { it.status }) { harRett, brukersInnsendteDagStatus ->
            when (harRett) {
                true -> {
                    require(brukersInnsendteDagStatus != MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                        "Når girRett er true, kan ikke status være IKKE_RETT_TIL_TILTAKSPENGER. sakId: $sakId, saksnummer: $saksnummer, meldekortId: $id, periode: $periode, meldeperiodeId: ${meldeperiode.id}"
                    }
                }

                false -> {
                    require(brukersInnsendteDagStatus == MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                        "Når girRett er false, må status være IKKE_RETT_TIL_TILTAKSPENGER. meldekortDager: $dager, meldeperiodeGirRett: ${meldeperiode.girRett}, sakId: $sakId, saksnummer: $saksnummer, meldekortId: $id, periode: $periode, meldeperiodeId: ${meldeperiode.id}"
                    }
                }
            }
        }

        if (mottatt == null) {
            require(journalføringstidspunkt == null)
            require(journalpostId == null)
        }

        if (mottatt != null) {
            require(!periode.tilOgMed.isAfter(senesteTilOgMedDatoForInnsending())) {
                "Meldekortet er ikke klart for innsending fra bruker"
            }
            require(deaktivert == null) {
                "Meldekort ${this.id} kan ikke være både mottatt og deaktivert"
            }
            if (meldeperiode.alleDagerGirRettIPeriode) {
                require(antallDagerBesvart == maksAntallDagerForPeriode) {
                    "Når alle dagene i en meldeperiode gir rett til tiltakspenger, må bruker besvare maksAntallDagerSomGirRett (fra vedtaket). Antall dager besvart: $antallDagerBesvart, maks antall dager for periode: ${meldeperiode.maksAntallDagerForPeriode}. sakId: $sakId, saksnummer: $saksnummer, periode: $periode, meldeperiodeId: ${meldeperiode.id}"
                }
            }
        }
    }

    companion object {
        const val DAGER_FØR_PERIODE_SLUTT_FOR_INNSENDING = 2L

        fun senesteTilOgMedDatoForInnsending(): LocalDate? =
            LocalDate.now().plusDays(DAGER_FØR_PERIODE_SLUTT_FOR_INNSENDING)
    }
}

fun Meldeperiode.tilTomtMeldekort(): Meldekort {
    return Meldekort(
        id = MeldekortId.random(),
        meldeperiode = this,
        mottatt = null,
        deaktivert = null,
        dager = this.girRett.tilMeldekortDager(),
        journalpostId = null,
        journalføringstidspunkt = null,
        varselId = null,
        erVarselInaktivert = false,
    )
}

fun Map<LocalDate, Boolean>.tilMeldekortDager(): List<MeldekortDag> = this.map {
    MeldekortDag(
        dag = it.key,
        status = if (it.value) MeldekortDagStatus.IKKE_BESVART else MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
    )
}

// TODO: etter hvert vil vi kanskje sende nytt varsel til bruker for oppdaterte meldekort
// da må vi sette varselId til null her, slik at nytt varsel kan genereres for meldekortet
fun Meldeperiode.tilOppdatertMeldekort(forrigeMeldekort: Meldekort): Meldekort {
    return forrigeMeldekort.copy(
        id = MeldekortId.random(),
        meldeperiode = this,
        mottatt = null,
        deaktivert = null,
        dager = forrigeMeldekort.dager.zip(this.girRett.values) { dag, harRett ->
            MeldekortDag(
                dag = dag.dag,
                status = when (harRett) {
                    true -> if (dag.status == MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) MeldekortDagStatus.IKKE_BESVART else dag.status
                    false -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                },
            )
        },
        journalpostId = null,
        journalføringstidspunkt = null,
    )
}
