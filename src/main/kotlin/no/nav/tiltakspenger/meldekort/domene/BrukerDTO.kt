package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import java.time.LocalDate

sealed interface BrukerDTO {
    val harSak: Boolean

    data class MedSak(
        val nesteMeldekort: MeldekortTilBrukerDTO?,
        val forrigeMeldekort: MeldekortTilBrukerDTO?,
        val nesteMeldeperiode: NesteMeldeperiodeDTO?,
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

data class NesteMeldeperiodeDTO(
    val kanSendes: LocalDate,
    val periode: PeriodeDTO,
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

private fun Bruker.MedSak.tilBrukerDTO(): BrukerDTO.MedSak {
    val nesteMeldekort = nesteMeldekort?.tilMeldekortTilBrukerDTO()
    val nesteMeldeperiode = sak.nesteMeldeperiode()?.let {
        NesteMeldeperiodeDTO(
            kanSendes = it.tilOgMed.minusDays(DAGER_FØR_PERIODE_SLUTT_FOR_INNSENDING),
            periode = it.toDTO(),
        )
    }

    return BrukerDTO.MedSak(
        nesteMeldekort = nesteMeldekort,
        forrigeMeldekort = sisteMeldekort?.tilMeldekortTilBrukerDTO(),
        nesteMeldeperiode = nesteMeldeperiode,
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
