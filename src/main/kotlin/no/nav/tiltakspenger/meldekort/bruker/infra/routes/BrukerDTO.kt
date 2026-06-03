package no.nav.tiltakspenger.meldekort.bruker.infra.routes

import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.bruker.Bruker
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.tilMeldekortTilBrukerDTO
import java.time.Clock

/**
 * I prod kun serialisert ut til frontend. Frontend bruker [harSak] for å diskriminere mellom de to variantene
 */
sealed interface BrukerDTO {
    val harSak: Boolean

    data class MedSak(
        val nesteMeldekort: MeldekortTilBrukerDTO?,
        val forrigeMeldekort: MeldekortTilBrukerDTO?,
        val arenaMeldekortStatus: ArenaMeldekortStatusDTO,
        val harSoknadUnderBehandling: Boolean,
        val kanSendeInnHelgForMeldekort: Boolean,
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

private fun Bruker.MedSak.tilBrukerDTO(clock: Clock): BrukerDTO.MedSak {
    val nesteMeldekort = nesteMeldekort?.tilMeldekortTilBrukerDTO(clock)

    return BrukerDTO.MedSak(
        nesteMeldekort = nesteMeldekort,
        forrigeMeldekort = sisteMeldekort?.tilMeldekortTilBrukerDTO(clock),
        arenaMeldekortStatus = sak.arenaMeldekortStatus.tilDTO(),
        harSoknadUnderBehandling = harSoknadUnderBehandling,
        kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
    )
}

private fun Bruker.UtenSak.tilBrukerDTO(): BrukerDTO.UtenSak = BrukerDTO.UtenSak(
    arenaMeldekortStatus = arenaMeldekortStatus.tilDTO(),
)

fun Bruker.tilBrukerDTO(clock: Clock): BrukerDTO = when (this) {
    is Bruker.MedSak -> tilBrukerDTO(clock)
    is Bruker.UtenSak -> tilBrukerDTO()
}
