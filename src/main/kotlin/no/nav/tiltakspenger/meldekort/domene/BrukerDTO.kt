package no.nav.tiltakspenger.meldekort.domene

import kotlinx.datetime.LocalDate

data class BrukerDTO(
    val nesteMeldekort: MeldekortTilBrukerDTO?,
    val sisteMeldekort: MeldekortTilBrukerDTO?,
    val nesteInnsending: LocalDate?,
    val arenaMeldekortStatus: ArenaMeldekortStatusDTO,
)

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

private fun Bruker.MedSak.tilBrukerDTO(): BrukerDTO = BrukerDTO(
    nesteMeldekort = nesteMeldekort?.tilMeldekortTilBrukerDTO(),
    sisteMeldekort = sisteMeldekort?.tilMeldekortTilBrukerDTO(),
    // TODO: sett dato for neste meldekort som kan sendes
    nesteInnsending = null,
    arenaMeldekortStatus = sak.arenaMeldekortStatus.tilDTO(),
)

private fun Bruker.UtenSak.tilBrukerDTO(): BrukerDTO = BrukerDTO(
    nesteMeldekort = null,
    sisteMeldekort = null,
    nesteInnsending = null,
    arenaMeldekortStatus = arenaMeldekortStatus.tilDTO(),
)

fun Bruker.tilBrukerDTO(): BrukerDTO = when (this) {
    is Bruker.MedSak -> tilBrukerDTO()
    is Bruker.UtenSak -> tilBrukerDTO()
}
