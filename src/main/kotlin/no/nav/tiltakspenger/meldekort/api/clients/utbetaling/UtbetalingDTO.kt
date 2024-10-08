package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning
import no.nav.tiltakspenger.meldekort.api.domene.UtbetalingStatus
import java.time.LocalDate
import java.util.UUID

data class UtbetalingDTO(
    val sakId: String,
    val utløsendeMeldekortId: UUID,
    val utbetalingDager: List<UtbetalingDagDTO>,
    val saksbehandler: String,
)

data class UtbetalingDagDTO(
    val dato: LocalDate,
    // TODO jah: Føles ikke riktig at denne ligger på dag, spesielt på de dagene det ikke er utbetaling og hva med de dagene det er mer enn 1 tiltak?
    val tiltaktype: String?,
    val status: UtbetalingDagStatusDTO,
    val meldekortId: UUID,
    val løpenr: Int,
)

enum class UtbetalingDagStatusDTO {
    IngenUtbetaling,
    FullUtbetaling,
    DelvisUtbetaling,
}

fun mapGrunnlag(
    behandlingId: String,
    fom: LocalDate,
    tom: LocalDate,
) = GrunnlagDTO(
    behandlingId = behandlingId,
    fom = fom,
    tom = tom,
)

fun mapUtbetalingMeldekort(
    sakId: String,
    behandling: MeldekortBeregning,
) = UtbetalingDTO(
    sakId = sakId,
    utløsendeMeldekortId = behandling.utløsendeMeldekortId,
    utbetalingDager =
    behandling.utbetalingDager.map {
        UtbetalingDagDTO(
            dato = it.dag,
            tiltaktype = it.tiltakType?.name,
            status =
            when (it.status) {
                UtbetalingStatus.FullUtbetaling -> UtbetalingDagStatusDTO.FullUtbetaling
                UtbetalingStatus.DelvisUtbetaling -> UtbetalingDagStatusDTO.DelvisUtbetaling
                UtbetalingStatus.IngenUtbetaling -> UtbetalingDagStatusDTO.IngenUtbetaling
            },
            meldekortId = it.meldekortId,
            løpenr = it.løpenr,
        )
    },
    saksbehandler = behandling.saksbehandler,
)
