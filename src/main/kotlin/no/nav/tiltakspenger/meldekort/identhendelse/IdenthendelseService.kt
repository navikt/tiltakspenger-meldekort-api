package no.nav.tiltakspenger.meldekort.identhendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import java.util.UUID

class IdenthendelseService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
) {
    private val log = KotlinLogging.logger { }

    fun behandleIdenthendelse(id: UUID, identhendelseDto: IdenthendelseDto) {
        val gammeltFnr = Fnr.fromString(identhendelseDto.gammeltFnr)
        val nyttFnr = Fnr.fromString(identhendelseDto.nyttFnr)
        meldeperiodeRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)
        log.info { "Oppdatert fnr for identhendelse med id $id" }
    }
}
