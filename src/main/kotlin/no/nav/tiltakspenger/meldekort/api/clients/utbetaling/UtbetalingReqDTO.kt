package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import java.time.LocalDate
import java.util.*

data class UtbetalingReqDTO(
    val meldekortId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val meldekortDager: List<MeldekortDagDTO>, // Egen DTO?
    val saksbehandler: String,
)

data class MeldekortDagDTO(
    val dato: LocalDate,
    val tiltak: TiltakDTO?,
    val status: MeldekortDagStatusDTO,
)

data class TiltakDTO(
    val id: UUID,
    val periode: PeriodeDTO,
    val typeBeskrivelse: String,
    val typeKode: String,
    val antDagerIUken: Float,
)

data class PeriodeDTO(
    val fra: LocalDate,
    val til: LocalDate,
)

enum class MeldekortDagStatusDTO(status: String) {
    IKKE_UTFYLT("Ikke utfylt"),
    DELTATT("Deltatt"),
    IKKE_DELTATT("Ikke deltatt"),
    FRAVÆR_SYK("Fravær syk"),
    FRAVÆR_SYKT_BARN("Fravær sykt barn"),
    FRAVÆR_VELFERD("Fravær velferd"),
    LØNN_FOR_TID_I_ARBEID("Lønn for tid i arbeid"),
}

fun mapUtbetalingMeldekort(meldekort: Meldekort.Innsendt) =
    UtbetalingReqDTO(
        meldekortId = meldekort.id,
        fom = meldekort.fom,
        tom = meldekort.tom,
        meldekortDager = meldekort.meldekortDager.map {
            MeldekortDagDTO(
                dato = it.dato,
                tiltak = it.tiltak?.let { tiltak ->
                    TiltakDTO(
                        id = tiltak.id,
                        periode = PeriodeDTO(
                            fra = tiltak.periode.fra,
                            til = tiltak.periode.til,
                        ),
                        typeBeskrivelse = tiltak.typeBeskrivelse,
                        typeKode = tiltak.typeKode,
                        antDagerIUken = tiltak.antDagerIUken,
                    )
                },
                status = MeldekortDagStatusDTO.valueOf(it.status.name),
            )
        },
        saksbehandler = meldekort.saksbehandler,
    )
