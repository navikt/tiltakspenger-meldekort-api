package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
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
    val dager: List<MeldekortDagFraBruker>,
    val mottatt: LocalDateTime,
)

data class MeldekortFraBrukerDTO(
    val id: String,
    val dager: List<MeldekortDagFraBruker>,
) {
    fun tilLagreKommando(fnr: Fnr): LagreMeldekortFraBrukerKommando {
        return LagreMeldekortFraBrukerKommando(
            id = MeldekortId.fromString(id),
            fnr = fnr,
            dager = dager,
            mottatt = nå(),
        )
    }
}

data class MeldekortDagFraBruker(
    override val dag: LocalDate,
    override val status: MeldekortDagStatus,
) : IMeldekortDag {
    fun tilMeldekortDag(): MeldekortDag = MeldekortDag(dag = dag, status = status)
}

fun Meldekort.validerLagring(kommando: LagreMeldekortFraBrukerKommando) {
    require(!meldeperiode.periode.tilOgMed.isAfter(senesteTilOgMedDatoForInnsending())) {
        "Meldekortet er ikke klart for innsending fra bruker"
    }

    val antallDagerRegistrert = kommando.dager.count { it.status != MeldekortDagStatus.IKKE_REGISTRERT }

    require(antallDagerRegistrert > 0) {
        "Meldekortet må ha minst en dag med registrering"
    }

    val maksAntallDager = meldeperiode.maksAntallDagerForPeriode

    require(antallDagerRegistrert <= maksAntallDager) {
        "Antall registrerte dager ($antallDagerRegistrert) overskrider maks ($maksAntallDager)"
    }

    val harRegistrerteDagerUtenRett = kommando.dager.any {
        it.status != MeldekortDagStatus.IKKE_REGISTRERT && meldeperiode.girRett[it.dag] == false
    }

    require(!harRegistrerteDagerUtenRett) {
        "Meldekortet har registering på dager uten rett - $harRegistrerteDagerUtenRett"
    }
}
