package no.nav.tiltakspenger.meldekort.identhendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.meldekort.sak.SakRepo

class IdenthendelseService(
    private val sakRepo: SakRepo,
) {
    private val log = KotlinLogging.logger { }

    fun behandleIdenthendelse(identhendelse: Identhendelse) {
        sakRepo.oppdaterFnr(gammeltFnr = identhendelse.gammeltFnr, nyttFnr = identhendelse.nyttFnr)
        log.info { "Oppdatert fnr for identhendelse med id ${identhendelse.id}" }
    }
}
