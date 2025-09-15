package no.nav.tiltakspenger.meldekort.domene.microfrontend

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.microfrontend.TmsMikrofrontendClient
import no.nav.tiltakspenger.meldekort.repository.SakRepo
import java.time.Clock

class InaktiverMicrofrontendJob(
    private val sakRepo: SakRepo,
    private val tmsMikrofrontendClient: TmsMikrofrontendClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    fun inaktiverMicrofrontendForBrukere() {
        Either.catch {
            val saker = sakRepo.hentSakerHvorMicrofrontendSkalInaktiveres(clock = clock)
            log.debug { "Fant ${saker.size} saker hvor microfrontend kan inaktiveres" }

            saker.forEach { sak ->
                Either.catch {
                    tmsMikrofrontendClient.inaktiverMicrofrontendForBruker(sak.fnr, sak.id)
                    sakRepo.oppdaterStatusForMicrofrontend(sakId = sak.id, aktiv = false)
                    log.info { "Microfrontend inaktivert for bruker med sak sakId=${sak.id}" }
                }.onLeft {
                    log.error(it) { "Kunne ikke inaktivere microfrontend for bruker med sakId=${sak.id}, prøver igjen neste jobbkjøring" }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under inaktivering av microfrontends" }
        }
    }
}
