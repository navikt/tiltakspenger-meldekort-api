package no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling

import java.time.LocalDate
import java.time.LocalDateTime

// TODO: flytt til libs
data class SakDTO(
    val fnr: String,
    val sakId: String,
    val saksnummer: String,
    val meldeperioder: List<MeldeperiodeDTO>,
) {

    data class MeldeperiodeDTO(
        val id: String,
        val kjedeId: String,
        val versjon: Int,
        val opprettet: LocalDateTime,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val antallDagerForPeriode: Int,
        val girRett: Map<LocalDate, Boolean>,
    )
}
