package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import java.time.LocalDate
import java.util.*

data class UtbetalingReqDTO(
    val meldekortId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val meldekortDager: List<MeldekortDag>, // Egen DTO?
    val saksbehandler: String,
)

fun mapUtbetalingMeldekort(meldekort: Meldekort.Innsendt) =
    UtbetalingReqDTO(
        meldekortId = meldekort.id,
        fom = meldekort.fom,
        tom = meldekort.tom,
        meldekortDager = meldekort.meldekortDager,
        saksbehandler = meldekort.saksbehandler,
    )
