package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning
import no.nav.tiltakspenger.meldekort.api.domene.UtbetalingStatus
import java.time.LocalDate
import java.util.UUID

data class UtbetalingDTO(
    val behandlingId: UUID,
    val utbetalingDager: List<UtbetalingDagDTO>,
    val saksbehandler: String,
)

data class UtbetalingDagDTO(
    val dato: LocalDate,
    val tiltaktype: String,
    val status: UtbetalingDagStatusDTO,
)

enum class UtbetalingDagStatusDTO {
    IngenUtbetaling,
    FullUtbetaling,
    DelvisUtbetaling,
}

fun mapUtbetalingMeldekort(behandling: MeldekortBeregning) =
    UtbetalingDTO(
        behandlingId = behandling.meldekortId,
        utbetalingDager = behandling.utbetalingDager.map {
            UtbetalingDagDTO(
                dato = it.dag,
                tiltaktype = it.tiltakType,
                status = when (it.status) {
                    UtbetalingStatus.FullUtbetaling -> UtbetalingDagStatusDTO.FullUtbetaling
                    UtbetalingStatus.DelvisUtbetaling -> UtbetalingDagStatusDTO.DelvisUtbetaling
                    UtbetalingStatus.IngenUtbetaling -> UtbetalingDagStatusDTO.IngenUtbetaling
                },
            )
        },
        saksbehandler = behandling.saksbehandler,
    )
