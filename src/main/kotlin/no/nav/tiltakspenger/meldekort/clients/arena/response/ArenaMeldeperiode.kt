package no.nav.tiltakspenger.meldekort.clients.arena.response

import java.time.LocalDate

data class ArenaMeldeperiode(
    val fra: LocalDate,
    val til: LocalDate,
    val kortKanSendesFra: LocalDate,
    val kanKortSendes: Boolean,
    val periodeKode: String,
)
