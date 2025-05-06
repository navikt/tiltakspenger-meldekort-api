package no.nav.tiltakspenger.meldekort.clients.arena

import java.time.LocalDate

data class ArenaMeldekortResponse(
    val personId: Long,
    val etternavn: String,
    val fornavn: String,
    val maalformkode: String,
    val meldeform: String,
    val meldekortListe: List<ArenaMeldekort>? = null,
    val antallGjenstaaendeFeriedager: Int? = 0,
    val fravaerListe: List<ArenaFravaerType>? = null,
)

data class ArenaMeldekort(
    val meldekortId: Long,
    val kortType: String,
    val meldeperiode: String,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val hoyesteMeldegruppe: String,
    val beregningstatus: String,
    val forskudd: Boolean,
    val mottattDato: LocalDate? = null,
    val bruttoBelop: Float = 0F,
)

data class ArenaFravaerType(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val type: String,
)
