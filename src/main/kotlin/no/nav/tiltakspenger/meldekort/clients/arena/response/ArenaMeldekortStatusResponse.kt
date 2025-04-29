package no.nav.tiltakspenger.meldekort.clients.arena.response

import java.time.LocalDate

data class ArenaMeldekortStatusResponse(
    val meldekort: Long,
    val etterregistrerteMeldekort: Long,
    val antallGjenstaaendeFeriedager: Long,
    val nesteMeldekort: ArenaMeldekortStatusNesteMeldekort,
    val nesteInnsendingAvMeldekort: LocalDate,
)

data class ArenaMeldekortStatusNesteMeldekort(
    val uke: String,
    val kanSendesFra: LocalDate,
    val til: LocalDate,
    val fra: LocalDate,
)
