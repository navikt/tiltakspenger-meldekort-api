package no.nav.tiltakspenger.meldekort.clients.arena.response

import java.time.LocalDate

data class ArenaMeldekort(
    val meldekortId: Long,
    val kortType: String,
    val meldeperiode: ArenaMeldeperiode,
    val meldegruppe: String,
    val kortStatus: String,
    val bruttoBelop: Double,
    val erForskuddsPeriode: Boolean,
    val mottattDato: LocalDate,
    val korrigerbart: Boolean,
)
