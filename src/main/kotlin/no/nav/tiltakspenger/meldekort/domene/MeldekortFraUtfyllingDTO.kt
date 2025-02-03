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
    val dager: List<MeldekortDagFraUtfylling>,
    val mottatt: LocalDateTime,
)

data class MeldekortFraUtfyllingDTO(
    val id: String,
    val dager: List<MeldekortDagFraUtfylling>,
) {
    fun toDomain(): LagreMeldekortFraBrukerKommando {
        return LagreMeldekortFraBrukerKommando(
            id = MeldekortId.fromString(id),
            dager = dager,
            mottatt = nå(),
        )
    }
}

data class MeldekortDagFraUtfylling(
    override val dag: LocalDate,
    override val status: MeldekortDagStatus,
) : MeldekortDag

fun BrukersMeldekort.validerLagring(kommando: LagreMeldekortFraBrukerKommando) {
    val maksAntallDager = meldeperiode.maksAntallDagerForPeriode
    val antallDagerRegistrert = kommando.dager.count { it.status != MeldekortDagStatus.IKKE_REGISTRERT }

    require(antallDagerRegistrert <= maksAntallDager) {
        "Antall registrerte dager ($antallDagerRegistrert) overskrider maks ($maksAntallDager)"
    }

    val registrerteDagerUtenRett = kommando.dager.filter {
        it.status != MeldekortDagStatus.IKKE_REGISTRERT && meldeperiode.girRett[it.dag] == false
    }

    require(registrerteDagerUtenRett.isNotEmpty()) {
        "Meldekortet har registering på dager uten rett - $registrerteDagerUtenRett"
    }
}
