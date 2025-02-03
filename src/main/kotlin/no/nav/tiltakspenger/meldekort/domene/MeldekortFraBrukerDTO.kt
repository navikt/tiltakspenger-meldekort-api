package no.nav.tiltakspenger.meldekort.domene

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
    val dager: List<MeldekortDagFraBruker>,
    val mottatt: LocalDateTime,
)

data class MeldekortFraBrukerDTO(
    val id: String,
    val dager: List<MeldekortDagFraBruker>,
) {
    fun tilLagreKommando(): LagreMeldekortFraBrukerKommando {
        return LagreMeldekortFraBrukerKommando(
            id = MeldekortId.fromString(id),
            dager = dager,
            mottatt = nå(),
        )
    }
}

data class MeldekortDagFraBruker(
    override val dag: LocalDate,
    override val status: MeldekortDagStatus,
) : IMeldekortDag

fun Meldekort.validerLagring(kommando: LagreMeldekortFraBrukerKommando) {
    val maksAntallDager = meldeperiode.maksAntallDagerForPeriode
    val antallDagerRegistrert = kommando.dager.count { it.status != MeldekortDagStatus.IKKE_REGISTRERT }

    require(antallDagerRegistrert <= maksAntallDager) {
        "Antall registrerte dager ($antallDagerRegistrert) overskrider maks ($maksAntallDager)"
    }

    val registrerteDagerUtenRett = kommando.dager.filter {
        it.status != MeldekortDagStatus.IKKE_REGISTRERT && meldeperiode.girRett[it.dag] == false
    }

    require(registrerteDagerUtenRett.isEmpty()) {
        "Meldekortet har registering på dager uten rett - $registrerteDagerUtenRett"
    }
}
