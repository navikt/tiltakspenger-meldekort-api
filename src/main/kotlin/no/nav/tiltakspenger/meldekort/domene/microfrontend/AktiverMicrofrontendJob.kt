package no.nav.tiltakspenger.meldekort.domene.microfrontend

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.microfrontend.TmsMikrofrontendClient
import no.nav.tiltakspenger.meldekort.repository.SakRepo
import java.time.Clock

class AktiverMicrofrontendJob(
    private val sakRepo: SakRepo,
    private val tmsMikrofrontendClient: TmsMikrofrontendClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    fun aktiverMicrofrontendForBrukere() {
        Either.catch {
            val saker = sakRepo.hentSakerHvorMicrofrontendSkalAktiveres(clock = clock)
            log.debug { "Fant ${saker.size} saker hvor microfrontend kan aktiveres" }

            saker.forEach { sak ->
                Either.catch {
                    tmsMikrofrontendClient.aktiverMicrofrontendForBruker(sak.fnr, sak.id)
                    sakRepo.oppdaterErMicrofrontendInaktivert(sakId = sak.id, erMicrofrontendInaktivert = false)
                    log.info { "Microfrontend aktivert for bruker med sak sakId=${sak.id}" }
                }.onLeft {
                    log.error(it) { "Kunne ikke aktivere microfrontend for bruker med sakId=${sak.id}, prøver igjen neste jobbkjøring" }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under aktivering av microfrontends" }
        }
    }
}
