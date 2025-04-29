package no.nav.tiltakspenger.meldekort.clients.arena.response

import java.time.LocalDate

data class ArenaNesteMeldekortResponse(
    val maalformkode: String,
    val meldeform: String,
    val meldekort: List<ArenaMeldekort>,
    val etterregistrerteMeldekort: List<ArenaMeldekort>,
    val fravaer: List<ArenaFravaerPeriode>,
    val id: String,
    val antallGjenstaaendeFeriedager: Long,
)

data class ArenaFravaerPeriode(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val type: String,
)
