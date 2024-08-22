package no.nav.tiltakspenger.meldekort.api.clients.dokument

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DokumentMeldekortDTO(
    val meldekortId: UUID,
    val sakId: String,
    val meldekortPeriode: PeriodeDTO,
    val saksbehandler: String,
    val meldekortDager: List<MeldekortDagDTO>,
    val tiltak: List<TiltakDTO>,
    val innsendingTidspunkt: LocalDateTime,
    val personopplysninger: PersonopplysningerDTO,
)

data class PersonopplysningerDTO(
    val fornavn: String,
    val etternavn: String,
    val ident: String,
)

data class PeriodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class TiltakDTO(
    val id: UUID,
    val periode: PeriodeDTO,
    // TODO jah: Venter på å rename denne for ikke å brekke tiltakstype-dokument.
    val typeKode: TiltakstypeSomGirRett,
    val antDagerIUken: Int,
)

data class MeldekortDagDTO(
    val dato: LocalDate,
    val tiltakType: TiltakstypeSomGirRett?,
    val status: MeldekortDagStatusTilDokumentDTO,
)

enum class MeldekortDagStatusTilDokumentDTO(status: String) {
    SPERRET("sperret"),
    IKKE_UTFYLT("Ikke utfylt"),
    DELTATT_UTEN_LØNN_I_TILTAKET("Deltatt uten lønn i tiltaket"),
    DELTATT_MED_LØNN_I_TILTAKET("Deltatt med lønn i tiltaket"),
    IKKE_DELTATT("Ikke deltatt i tiltaket"),
    FRAVÆR_SYK("Fravær - Syk"),
    FRAVÆR_SYKT_BARN("Fravær - Sykt barn"),
    FRAVÆR_VELFERD_GODKJENT_AV_NAV("Fravær - Velferd. Godkjent av NAV"),
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV("Fravær - Velferd. Ikke godkjent av NA"),
}

fun mapMeldekortDTOTilDokumentDTO(meldekort: Meldekort?, grunnlag: MeldekortGrunnlag): DokumentMeldekortDTO {
    if (meldekort !is Meldekort.Innsendt) {
        throw IllegalStateException("Meldekortet eksisterer ikke")
    }

    return DokumentMeldekortDTO(
        meldekortId = meldekort.id,
        meldekortPeriode = PeriodeDTO(
            fom = meldekort.fom,
            tom = meldekort.tom,
        ),
        sakId = grunnlag.sakId,
        saksbehandler = meldekort.saksbehandler,
        tiltak = grunnlag.tiltak.map {
            TiltakDTO(
                id = it.id,
                periode = PeriodeDTO(it.periode.fra, it.periode.til),
                typeKode = it.tiltakstype,
                antDagerIUken = it.antDagerIUken,
            )
        },
        meldekortDager = meldekort.meldekortDager.map {
            MeldekortDagDTO(
                dato = it.dato,
                tiltakType = it.tiltak?.tiltakstype,
                status = when (it.status) {
                    MeldekortDagStatus.SPERRET -> MeldekortDagStatusTilDokumentDTO.SPERRET
                    MeldekortDagStatus.IKKE_UTFYLT -> MeldekortDagStatusTilDokumentDTO.IKKE_UTFYLT
                    MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatusTilDokumentDTO.DELTATT_UTEN_LØNN_I_TILTAKET
                    MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatusTilDokumentDTO.DELTATT_MED_LØNN_I_TILTAKET
                    MeldekortDagStatus.IKKE_DELTATT -> MeldekortDagStatusTilDokumentDTO.IKKE_DELTATT
                    MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatusTilDokumentDTO.FRAVÆR_SYK
                    MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatusTilDokumentDTO.FRAVÆR_SYKT_BARN
                    MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> MeldekortDagStatusTilDokumentDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV
                    MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> MeldekortDagStatusTilDokumentDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
                },
            )
        },
        innsendingTidspunkt = meldekort.sendtInn,
        personopplysninger = PersonopplysningerDTO(
            ident = grunnlag.personopplysninger.ident,
            fornavn = grunnlag.personopplysninger.fornavn,
            etternavn = grunnlag.personopplysninger.etternavn,
        ),
    )
}
