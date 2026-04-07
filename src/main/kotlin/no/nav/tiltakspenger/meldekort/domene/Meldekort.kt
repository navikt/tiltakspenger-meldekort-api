@file:Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")

package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.periode.Periode
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
 *
 */
data class Meldekort(
    val id: MeldekortId,
    val deaktivert: LocalDateTime?,
    val mottatt: LocalDateTime?,
    val meldeperiode: Meldeperiode,
    val dager: List<MeldekortDag>,
    val journalpostId: JournalpostId?,
    val journalføringstidspunkt: LocalDateTime?,
    val varselId: VarselId,
    val erVarselInaktivert: Boolean,
    /**
     * Nullable fordi vi ikke vet nøyaktig tidspunkt for allerede sendte varsler i databasen.
     * Når [sendtVarselTidspunkt] er satt betyr det at varselet er sendt.
     */
    val sendtVarselTidspunkt: LocalDateTime?,
    val sendtVarsel: Boolean,
    /**
     * Ettersom meldekort ikke synkroniseres tilbake fra tiltakspenger-saksbehandling-api så vet vi ikke helt om meldekortet er en korrigering eller ikke.
     * Saksbehandler kan ha sendt inn meldekort for samme periode slik at brukes første innsendte meldekort for en gitt periode egentlig er en korrigering.
     */
    val korrigering: Boolean,
    val locale: String?,
) {
    val sakId = meldeperiode.sakId
    val periode: Periode = meldeperiode.periode
    val fnr: Fnr = meldeperiode.fnr
    val saksnummer: String = meldeperiode.saksnummer

    val erInnsendt: Boolean by lazy { mottatt != null }

    fun status(clock: Clock): MeldekortStatus = when {
        erInnsendt -> MeldekortStatus.INNSENDT
        deaktivert != null -> MeldekortStatus.DEAKTIVERT
        (LocalDateTime.now(clock) >= meldeperiode.kanFyllesUtFraOgMed) -> MeldekortStatus.KAN_UTFYLLES
        else -> MeldekortStatus.IKKE_KLAR
    }

    /**
     * Om den er klar til innsending nå.
     * OBS! Denne dupliserer logikken som finnes for [no.nav.tiltakspenger.meldekort.repository.MeldekortPostgresRepo.hentAlleMeldekortKlarTilInnsending]. Om den ene endres så burde begge endres
     */
    fun klarTilInnsending(clock: Clock): Boolean = status(clock) == MeldekortStatus.KAN_UTFYLLES

    /** Hvilken dag de er klar til innsending eller null dersom den aldri kan sendes inn. */
    fun klarTilInnsendingDateTime(clock: Clock): LocalDateTime? = when (status(clock)) {
        MeldekortStatus.KAN_UTFYLLES,
        MeldekortStatus.IKKE_KLAR,
        -> meldeperiode.kanFyllesUtFraOgMed

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
        korrigering: Boolean,
        locale: String?,
    ): Meldekort {
        if (erInnsendt) {
            throw IllegalArgumentException("Meldekort med id ${this.id} er allerede mottatt ($mottatt)")
        }
        require(this.status(clock) == MeldekortStatus.KAN_UTFYLLES) {
            "Meldekort med id ${this.id} er ikke klar til innsending (status: ${this.status(clock)})"
        }
        return this.copy(
            meldeperiode = sisteMeldeperiode,
            mottatt = LocalDateTime.now(clock),
            dager = brukerutfylteDager,
            korrigering = korrigering,
            locale = locale,
        )
    }

    fun fyllUtMeldekortFraBruker(
        brukerutfylteDager: List<MeldekortDag>,
        locale: String?,
        clock: Clock,
    ): Meldekort {
        if (erInnsendt) {
            throw IllegalArgumentException("Meldekort med id ${this.id} er allerede mottatt ($mottatt)")
        }
        require(this.status(clock) == MeldekortStatus.KAN_UTFYLLES) {
            "Meldekort med id ${this.id} er ikke klar til innsending (status: ${this.status(clock)})"
        }

        return this.copy(
            mottatt = LocalDateTime.now(clock),
            dager = brukerutfylteDager,
            locale = locale,
        )
    }

    fun inaktiverVarsel(): Meldekort {
        return this.copy(
            erVarselInaktivert = true,
        )
    }

    init {
        val msgSuffix = "(id: $id - mpId: ${meldeperiode.id} - kjedeId: ${meldeperiode.kjedeId} - dager: $dager)"

        dager.zipWithNext().forEach { (dag, nesteDag) ->
            require(dag.dag.isBefore(nesteDag.dag)) { "Dager må være sortert $msgSuffix" }
        }
        require(dager.first().dag == periode.fraOgMed) { "Første dag i meldekortet må være lik første dag i meldeperioden $msgSuffix" }
        require(dager.last().dag == periode.tilOgMed) { "Siste dag i meldekortet må være lik siste dag i meldeperioden $msgSuffix" }
        require(dager.size.toLong() == periode.antallDager) { "Antall dager i meldekortet må være lik antall dager i meldeperioden $msgSuffix" }
        require(meldeperiode.girRett.values.any { it }) { "Meldeperioden for meldekortet må ha minst en dag som gir rett $msgSuffix" }

        dager.forEach {
            val harRett = meldeperiode.girRett[it.dag]!!

            if (harRett) {
                require(it.status != MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                    "Når girRett er true, kan ikke status være IKKE_RETT_TIL_TILTAKSPENGER $msgSuffix"
                }
            } else {
                require(it.status == MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                    "Når girRett er false, må status være IKKE_RETT_TIL_TILTAKSPENGER $msgSuffix"
                }
            }
        }

        if (mottatt == null) {
            require(journalføringstidspunkt == null)
            require(journalpostId == null)
        }
        if (sendtVarselTidspunkt != null) {
            require(sendtVarsel)
        }

        if (erInnsendt) {
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
        varselId = VarselId.random(),
        erVarselInaktivert = false,
        sendtVarselTidspunkt = null,
        sendtVarsel = false,
        korrigering = false,
        locale = null,
    )
}

fun Map<LocalDate, Boolean>.tilMeldekortDager(): List<MeldekortDag> = this.map {
    MeldekortDag(
        dag = it.key,
        status = if (it.value) MeldekortDagStatus.IKKE_BESVART else MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
    )
}

fun Meldeperiode.tilOppdatertMeldekort(forrigeMeldekort: Meldekort?): Meldekort? {
    if (!this.minstEnDagGirRettIPerioden) {
        return null
    }
    if (forrigeMeldekort == null) {
        return this.tilTomtMeldekort()
    }
    // Ikke lag et nytt meldekort dersom meldekortet allerede var mottatt
    // Bruker må selv opprette en korrigering dersom det er endringer som påvirker allerede innsendte meldekort
    if (forrigeMeldekort.erInnsendt) {
        return null
    }
    // Vi beholder samme varselId, sendtVarsel, sendtVarselTidspunkt uavhengig om det er varslet allerede eller ikke.
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
        // På grunn av "bug" i MeldekortPostgresRepo.deaktiver, kan vi ikke garantert vite om varselet er inaktivert enda eller skal inaktiveres. Så når vi mottar dette meldekortet eller det blir deaktivert, får vi muligheten til å inaktivere varselet. Det er null problem og "inaktivere" det 2 ganger.
        erVarselInaktivert = false,
        locale = null,
    )
}
