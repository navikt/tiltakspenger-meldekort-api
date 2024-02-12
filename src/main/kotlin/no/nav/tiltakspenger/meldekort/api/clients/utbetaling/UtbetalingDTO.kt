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

fun mapUtbetalingMeldekort(behandling: MeldekortBeregning) =
    UtbetalingDTO(
        sakId = "sak_01HGD8E4RY7KSZ1YVVB1NK1XGH", // Denne må følge med fra vedtak og inn på grunnlaget
        utløsendeMeldekortId = behandling.meldekortId,
        utbetalingDager = behandling.utbetalingDager.map {
            UtbetalingDagDTO(
                dato = it.dag,
                tiltaktype = it.tiltakType,
                status = when (it.status) {
                    UtbetalingStatus.FullUtbetaling -> UtbetalingDagStatusDTO.FullUtbetaling
                    UtbetalingStatus.DelvisUtbetaling -> UtbetalingDagStatusDTO.DelvisUtbetaling
                    UtbetalingStatus.IngenUtbetaling -> UtbetalingDagStatusDTO.IngenUtbetaling
                },
                meldekortId = behandling.meldekortId, // Denne må legges på hver dag
                løpenr = it.løpenr,
            )
        },
        saksbehandler = behandling.saksbehandler,
    )
