package no.nav.tiltakspenger.meldekort.routes.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import java.time.LocalDate

// TODO: WIP, hent fra libs etter hvert

enum class MeldekortStatusTilBrukerDTO {
    KAN_UTFYLLES,
    KAN_IKKE_UTFYLLES,
    GODKJENT,
}

enum class MeldekortDagStatusTilBrukerDTO {
    DELTATT_UTEN_LØNN,
    DELTATT_MED_LØNN,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_ANNET_MED_RETT,
    FRAVÆR_ANNET_UTEN_RETT,
    IKKE_DELTATT,
    IKKE_REGISTRERT,
    IKKE_RETT,
}

fun MeldekortDagStatusTilBrukerDTO.tilMeldekortDagStatus(): MeldekortDagStatus =
    when (this) {
        MeldekortDagStatusTilBrukerDTO.DELTATT_UTEN_LØNN -> MeldekortDagStatus.DELTATT
        MeldekortDagStatusTilBrukerDTO.DELTATT_MED_LØNN -> MeldekortDagStatus.DELTATT
        MeldekortDagStatusTilBrukerDTO.FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
        MeldekortDagStatusTilBrukerDTO.FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
        MeldekortDagStatusTilBrukerDTO.FRAVÆR_ANNET_MED_RETT -> MeldekortDagStatus.FRAVÆR_ANNET
        MeldekortDagStatusTilBrukerDTO.FRAVÆR_ANNET_UTEN_RETT -> MeldekortDagStatus.IKKE_DELTATT
        MeldekortDagStatusTilBrukerDTO.IKKE_DELTATT -> MeldekortDagStatus.IKKE_DELTATT
        MeldekortDagStatusTilBrukerDTO.IKKE_REGISTRERT -> MeldekortDagStatus.IKKE_REGISTRERT
        MeldekortDagStatusTilBrukerDTO.IKKE_RETT -> MeldekortDagStatus.SPERRET
    }

data class MeldekortDagTilBrukerDTO(
    val dag: LocalDate,
    val status: MeldekortDagStatusTilBrukerDTO,
    val tiltakstype: TiltakstypeSomGirRett
)

data class MeldekortTilBrukerDTO(
    val id: String,
    val fnr: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: MeldekortStatusTilBrukerDTO,
    val meldekortDager: List<MeldekortDagTilBrukerDTO>,
) {
    fun tilMeldekort(): Meldekort {
        return Meldekort(
            id = HendelseId.fromString(this.id),
            fnr = Fnr.fromString(this.fnr),
            fraOgMed = this.fraOgMed,
            tilOgMed = this.tilOgMed,
            meldeperiodeId = MeldeperiodeId("$fraOgMed/$tilOgMed"),
            status = when (this.status) {
                MeldekortStatusTilBrukerDTO.GODKJENT -> MeldekortStatus.GODKJENT
                MeldekortStatusTilBrukerDTO.KAN_UTFYLLES -> MeldekortStatus.KAN_UTFYLLES
                MeldekortStatusTilBrukerDTO.KAN_IKKE_UTFYLLES -> MeldekortStatus.KAN_IKKE_UTFYLLES
            },
            meldekortDager = this.meldekortDager.map {
                MeldekortDag(
                    dag = it.dag,
                    status = it.status.tilMeldekortDagStatus(),
                    tiltakstype = it.tiltakstype
                )
            },
        )
    }
}
