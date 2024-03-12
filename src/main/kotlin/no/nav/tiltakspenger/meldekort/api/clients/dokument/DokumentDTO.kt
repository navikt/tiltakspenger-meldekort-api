package no.nav.tiltakspenger.meldekort.api.clients.dokument

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
    val typeBeskrivelse: String,
    val typeKode: String,
    val antDagerIUken: Float,
)

data class MeldekortDagDTO(
    val dato: LocalDate,
    val tiltakType: String?,
    val status: MeldekortDagStatusDTO,
)

enum class MeldekortDagStatusDTO(status: String) {
    SPERRET("Sperret"),
    IKKE_UTFYLT("Ikke utfylt"),
    DELTATT("Deltatt"),
    IKKE_DELTATT("Ikke deltatt"),
    FRAVÆR_SYK("Fravær syk"),
    FRAVÆR_SYKT_BARN("Fravær sykt barn"),
    FRAVÆR_VELFERD("Fravær velferd"),
    LØNN_FOR_TID_I_ARBEID("Lønn for tid i arbeid"),
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
                typeKode = it.typeKode,
                typeBeskrivelse = it.typeBeskrivelse,
                antDagerIUken = it.antDagerIUken,
            )
        },
        meldekortDager = meldekort.meldekortDager.map {
            MeldekortDagDTO(
                dato = it.dato,
                tiltakType = it.tiltak?.typeKode,
                status = when (it.status) {
                    MeldekortDagStatus.SPERRET -> MeldekortDagStatusDTO.IKKE_UTFYLT
                    MeldekortDagStatus.DELTATT -> MeldekortDagStatusDTO.DELTATT
                    MeldekortDagStatus.IKKE_DELTATT -> MeldekortDagStatusDTO.IKKE_DELTATT
                    MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatusDTO.FRAVÆR_SYK
                    MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN
                    MeldekortDagStatus.IKKE_UTFYLT -> MeldekortDagStatusDTO.IKKE_UTFYLT
                    MeldekortDagStatus.FRAVÆR_VELFERD -> MeldekortDagStatusDTO.FRAVÆR_VELFERD
                    MeldekortDagStatus.LØNN_FOR_TID_I_ARBEID -> MeldekortDagStatusDTO.LØNN_FOR_TID_I_ARBEID
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
