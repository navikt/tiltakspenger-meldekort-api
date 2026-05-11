package no.nav.tiltakspenger.meldekort.identhendelse

import no.nav.tiltakspenger.libs.common.Fnr
import java.util.UUID

data class Identhendelse(
    val id: UUID,
    val gammeltFnr: Fnr,
    val nyttFnr: Fnr,
)
