package no.nav.tiltakspenger.meldekort.domene.microfrontend

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.microfrontend.TmsMikrofrontendClient
import no.nav.tiltakspenger.meldekort.repository.SakRepo

class InaktiverMicrofrontendJob(
    private val sakRepo: SakRepo,
    private val tmsMikrofrontendClient: TmsMikrofrontendClient,
) {
    private val log = KotlinLogging.logger { }

    fun inaktiverMicrofrontendForBrukere() {
        Either.catch {
            val saker = sakRepo.hentSakerHvorSistePeriodeMedRettighetErLengeSiden()
            log.debug { "Fant ${saker.size} saker hvor microfrontend kan inaktiveres" }

            saker.forEach { sak ->
                log.info { "Inaktiverer microfrontend for bruker med sakId=${sak.id}" }

                Either.catch {
                    tmsMikrofrontendClient.inaktiverMicrofrontendForBruker(sak.fnr, sak.id)
                    sakRepo.lagre(sak.copy(erMicrofrontendInaktivert = true))
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
