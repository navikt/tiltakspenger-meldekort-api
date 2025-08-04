package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Command-delen av CQRS for meldekort. Brukes for å lagre meldekort som bruker har fylt ut.
 *
 * @param id Id til spesifikk versjon av meldeperioden på denne saken.
 */
data class LagreMeldekortFraBrukerKommando(
    val id: MeldekortId,
    val fnr: Fnr,
    val dager: List<MeldekortDagFraBrukerDTO>,
    val mottatt: LocalDateTime,
)

data class MeldekortFraBrukerDTO(
    val id: String,
    val dager: List<MeldekortDagFraBrukerDTO>,
) {
    fun tilLagreKommando(fnr: Fnr, clock: Clock): LagreMeldekortFraBrukerKommando {
        return LagreMeldekortFraBrukerKommando(
            id = MeldekortId.fromString(id),
            fnr = fnr,
            dager = dager,
            mottatt = nå(clock),
        )
    }
}

data class MeldekortDagFraBrukerDTO(
    val dag: LocalDate,
    val status: MeldekortDagStatusDTO,
) {
    fun tilMeldekortDag(): MeldekortDag = MeldekortDag(
        dag = dag,
        status = status.tilMeldekortDagStatus(),
    )
}

fun Meldekort.validerLagring(meldeperiode: Meldeperiode, nyeDager: List<MeldekortDag>) {
    require(meldeperiode.girRett.keys.toList() == nyeDager.map { it.dag }) {
        "Forventer at meldeperioden sin girRett-verdier har samme dager som nyeDager. " +
            "Meldeperiode girRett: ${meldeperiode.girRett.keys}, nyeDager: ${nyeDager.map { it.dag }}"
    }
    require(!periode.tilOgMed.isAfter(Meldekort.senesteTilOgMedDatoForInnsending())) {
        "Meldekortet er ikke klart for innsending fra bruker"
    }

    val antallDagerRegistrert =
        nyeDager.count {
            it.status != MeldekortDagStatus.IKKE_BESVART &&
                it.status != MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER &&
                it.status != MeldekortDagStatus.IKKE_TILTAKSDAG
        }

    // Vi støtter 0 dager hvis antallIkkeRett er større eller lik maksAntallDager.
    require(antallDagerRegistrert >= meldeperiode.minAntallDagerForPeriode && antallDagerRegistrert <= meldeperiode.maksAntallDagerForPeriode) {
        "Antall registrerte dager ($antallDagerRegistrert) må være mellom $meldeperiode.minAntallDagerForPeriode og $meldeperiode.maksAntallDagerForPeriode"
    }

    meldeperiode.girRett.values.zip(nyeDager.map { it.status }) { harRett, brukersInnsendteDagStatus ->
        when (harRett) {
            true -> {
                require(brukersInnsendteDagStatus != MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                    "Når girRett er true, kan ikke status være IKKE_RETT_TIL_TILTAKSPENGER. GirRett: $harRett, Status: $brukersInnsendteDagStatus"
                }
            }

            false -> {
                require(brukersInnsendteDagStatus == MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                    "Når girRett er false, må status være IKKE_RETT_TIL_TILTAKSPENGER. GirRett: $harRett, Status: $brukersInnsendteDagStatus"
                }
            }
        }
    }
}
