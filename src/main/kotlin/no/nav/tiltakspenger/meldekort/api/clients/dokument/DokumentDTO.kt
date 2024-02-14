package no.nav.tiltakspenger.meldekort.api.clients.dokument

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.felles.Periode
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
    val personopplysninger: Personopplysninger,
)

data class Personopplysninger(
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
    val periode: Periode,
    val typeBeskrivelse: String,
    val typeKode: String,
    val antDagerIUken: Float,
)

data class MeldekortDagDTO(
    val dato: LocalDate,
    val tiltakType: String?,
    val status: MeldekortDagStatus,
)

fun mapMeldekortDTOTilDokumentDTO(meldekort: Meldekort?, grunnlag: MeldekortGrunnlag) {
    if (meldekort !is Meldekort.Innsendt) {
        throw IllegalStateException("Meldekortet eksisterer ikke")
    }

    DokumentMeldekortDTO(
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
                periode = it.periode,
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
                    MeldekortDagStatus.DELTATT -> MeldekortDagStatus.DELTATT
                    MeldekortDagStatus.IKKE_DELTATT -> MeldekortDagStatus.IKKE_DELTATT
                    MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
                    MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
                    MeldekortDagStatus.IKKE_UTFYLT -> MeldekortDagStatus.IKKE_UTFYLT
                    MeldekortDagStatus.FRAVÆR_VELFERD -> MeldekortDagStatus.FRAVÆR_VELFERD
                    MeldekortDagStatus.LØNN_FOR_TID_I_ARBEID -> MeldekortDagStatus.LØNN_FOR_TID_I_ARBEID
                },
            )
        },
        innsendingTidspunkt = meldekort.sendtInn,
        personopplysninger = Personopplysninger(
            ident = grunnlag.personopplysninger.ident,
            fornavn = grunnlag.personopplysninger.fornavn,
            etternavn = grunnlag.personopplysninger.etternavn,
        ),
    )
}
