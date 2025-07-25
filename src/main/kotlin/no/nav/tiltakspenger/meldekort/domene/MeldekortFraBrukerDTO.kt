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

fun Meldekort.validerLagring(kommando: LagreMeldekortFraBrukerKommando) {
    require(!periode.tilOgMed.isAfter(Meldekort.senesteTilOgMedDatoForInnsending())) {
        "Meldekortet er ikke klart for innsending fra bruker"
    }

    val antallDagerRegistrert = kommando.dager.count { it.status != MeldekortDagStatusDTO.IKKE_BESVART }

    require(antallDagerRegistrert > 0) {
        "Meldekortet må ha minst en dag med registrering"
    }

    val forventetAntallDager = meldeperiode.maksAntallDagerForPeriode

    require(antallDagerRegistrert == forventetAntallDager) {
        if (antallDagerRegistrert > forventetAntallDager) {
            "Antall registrerte dager ($antallDagerRegistrert) overskrider maks ($forventetAntallDager)"
        } else {
            "Antall registrerte dager ($antallDagerRegistrert) er færre enn forventet antall registrerte dager ($forventetAntallDager)"
        }
    }

    val harRegistrerteDagerUtenRett = kommando.dager.any {
        it.status != MeldekortDagStatusDTO.IKKE_BESVART && meldeperiode.girRett[it.dag] == false
    }

    require(!harRegistrerteDagerUtenRett) {
        "Meldekortet har registering på dager uten rett - $harRegistrerteDagerUtenRett"
    }
}
