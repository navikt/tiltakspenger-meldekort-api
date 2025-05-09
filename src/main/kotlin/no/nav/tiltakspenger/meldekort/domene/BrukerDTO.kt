package no.nav.tiltakspenger.meldekort.domene

import kotlinx.datetime.LocalDate

sealed interface BrukerDTO {
    val harSak: Boolean

    data class MedSak(
        val nesteMeldekort: MeldekortTilBrukerDTO?,
        val sisteMeldekort: MeldekortTilBrukerDTO?,
        val nesteInnsending: LocalDate?,
        val arenaMeldekortStatus: ArenaMeldekortStatusDTO,
    ) : BrukerDTO {
        override val harSak = true
    }

    data class UtenSak(
        val arenaMeldekortStatus: ArenaMeldekortStatusDTO,
    ) : BrukerDTO {
        override val harSak = false
    }
}

enum class ArenaMeldekortStatusDTO {
    UKJENT,
    HAR_MELDEKORT,
    HAR_IKKE_MELDEKORT,
}

fun ArenaMeldekortStatus.tilDTO(): ArenaMeldekortStatusDTO = when (this) {
    ArenaMeldekortStatus.UKJENT -> ArenaMeldekortStatusDTO.UKJENT
    ArenaMeldekortStatus.HAR_MELDEKORT -> ArenaMeldekortStatusDTO.HAR_MELDEKORT
    ArenaMeldekortStatus.HAR_IKKE_MELDEKORT -> ArenaMeldekortStatusDTO.HAR_IKKE_MELDEKORT
}

private fun Bruker.MedSak.tilBrukerDTO(): BrukerDTO.MedSak {
    val nesteMeldekort = nesteMeldekort?.tilMeldekortTilBrukerDTO()

    return BrukerDTO.MedSak(
        nesteMeldekort = nesteMeldekort,
        sisteMeldekort = sisteMeldekort?.tilMeldekortTilBrukerDTO(),
        // TODO: sett dato for neste meldekort som kan sendes
        nesteInnsending = null,
        arenaMeldekortStatus = sak.arenaMeldekortStatus.tilDTO(),
    )
}

private fun Bruker.UtenSak.tilBrukerDTO(): BrukerDTO.UtenSak = BrukerDTO.UtenSak(
    arenaMeldekortStatus = arenaMeldekortStatus.tilDTO(),
)

fun Bruker.tilBrukerDTO(): BrukerDTO = when (this) {
    is Bruker.MedSak -> tilBrukerDTO()
    is Bruker.UtenSak -> tilBrukerDTO()
}
