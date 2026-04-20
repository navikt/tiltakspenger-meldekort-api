package no.nav.tiltakspenger.meldekort.clients.arena

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import java.time.LocalDate
import java.time.LocalDateTime

data class ArenaMeldekortResponse(
    val personId: Long,
    val etternavn: String,
    val fornavn: String,
    val maalformkode: String,
    val meldeform: String,
    val antallGjenstaaendeFeriedager: Int? = 0,
    val meldekortListe: List<ArenaMeldekort>? = null,
    val fravaerListe: List<ArenaFravaerType>? = null,
) {

    fun harTiltakspengerMeldekort(): Boolean {
        return meldekortListe?.any { it.erTiltakspengerMeldekort() } ?: false
    }

    fun hentTiltakspengerMeldekort(): NonEmptyList<ArenaMeldekort>? {
        return meldekortListe
            ?.filter { it.erTiltakspengerMeldekort() }
            ?.toNonEmptyListOrNull()
    }
}

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
) {

    fun erTiltakspengerMeldekort(): Boolean {
        return hoyesteMeldegruppe == TILTAKSPENGER_MELDEGRUPPE
    }

    // Åpnes for innsending fra midnatt siste lørdag
    fun kanSendesFra(): LocalDateTime {
        return tilDato.minusDays(1).atStartOfDay()
    }

    companion object {
        private const val TILTAKSPENGER_MELDEGRUPPE = "INDIV"
    }
}

data class ArenaFravaerType(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val type: String,
)
