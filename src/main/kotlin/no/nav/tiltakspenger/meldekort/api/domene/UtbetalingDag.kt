package no.nav.tiltakspenger.meldekort.api.domene

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import java.time.LocalDate
import java.util.UUID

/**
 * @param tiltakType Settes til null dersom det er sperret (skal ikke utbetales for disse dagene)
 */
data class UtbetalingDag(
    val dag: LocalDate,
    val status: UtbetalingStatus,
    val løpenr: Int,
    val meldekortId: UUID,
    val tiltakType: TiltakstypeSomGirRett?,
)

enum class UtbetalingStatus {
    IngenUtbetaling,
    FullUtbetaling,
    DelvisUtbetaling,
}
