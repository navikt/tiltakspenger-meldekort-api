package no.nav.tiltakspenger.meldekort.identhendelse.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.identhendelse.Identhendelse
import java.util.UUID

data class IdenthendelseDto(
    val gammeltFnr: String,
    val nyttFnr: String,
)

fun IdenthendelseDto.tilIdenthendelse(id: UUID): Identhendelse =
    Identhendelse(
        id = id,
        gammeltFnr = Fnr.fromString(gammeltFnr),
        nyttFnr = Fnr.fromString(nyttFnr),
    )
