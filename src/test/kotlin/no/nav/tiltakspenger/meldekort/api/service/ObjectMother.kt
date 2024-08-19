package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.api.domene.Tiltak
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import java.time.LocalDate
import java.util.UUID

object ObjectMother {
    fun tiltak(
        id: UUID = UUID.randomUUID(),
        periode: Periode = Periode(fra = LocalDate.of(2024, 1, 1), til = LocalDate.of(2024, 1, 4)),
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        antDagerIUken: Int = 5,
    ) = Tiltak(
        id = id,
        periode = periode,
        tiltakstype = tiltakstype,
        antDagerIUken = antDagerIUken,
    )
}
