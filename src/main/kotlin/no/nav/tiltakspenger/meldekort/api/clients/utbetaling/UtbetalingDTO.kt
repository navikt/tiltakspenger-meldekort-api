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
    val tiltaktype: String,
    val status: UtbetalingDagStatusDTO,
    val meldekortId: UUID,
    val løpenr: Int,
)

enum class UtbetalingDagStatusDTO {
    IngenUtbetaling,
    FullUtbetaling,
    DelvisUtbetaling,
}

fun mapUtbetalingMeldekort(
    sakId: String,
    behandling: MeldekortBeregning,
) =
    UtbetalingDTO(
        sakId = sakId,
        utløsendeMeldekortId = behandling.utløsendeMeldekortId,
        utbetalingDager = behandling.utbetalingDager.map {
            UtbetalingDagDTO(
                dato = it.dag,
                tiltaktype = it.tiltakType,
                status = when (it.status) {
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
