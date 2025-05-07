package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.repository.SakRepo

class ArenaMeldekortStatusService(
    private val arenaMeldekortClient: ArenaMeldekortClient,
    private val sakRepo: SakRepo,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentArenaMeldekortStatus(fnr: Fnr): ArenaMeldekortStatus {
        arenaMeldekortClient.hentMeldekort(fnr).onLeft {
            logger.error { "Kunne ikke hente meldekort fra arena - $it" }
            return ArenaMeldekortStatus.UKJENT
        }.onRight {
            if (it == null) {
                return ArenaMeldekortStatus.HAR_IKKE_MELDEKORT
            } else if (it.harTiltakspengerMeldekort()) {
                return ArenaMeldekortStatus.HAR_MELDEKORT
            }
        }

        val historiskeMeldekort = arenaMeldekortClient.hentHistoriskeMeldekort(fnr).getOrElse {
            logger.error { "Kunne ikke hente historiske meldekort fra arena - $it" }
            return ArenaMeldekortStatus.UKJENT
        }

        return if (historiskeMeldekort?.harTiltakspengerMeldekort() != true) {
            ArenaMeldekortStatus.HAR_IKKE_MELDEKORT
        } else {
            ArenaMeldekortStatus.HAR_MELDEKORT
        }
    }

    suspend fun oppdaterArenaMeldekortStatusForSaker() {
        Either.catch {
            val saker = sakRepo.hentSakerUtenArenaStatus()

            logger.info { "Fant ${saker.size} saker med ukjent arena status" }

            saker.forEach { sak ->
                Either.catch {
                    val arenaMeldekortStatus = hentArenaMeldekortStatus(sak.fnr)
                    if (arenaMeldekortStatus != ArenaMeldekortStatus.UKJENT) {
                        sakRepo.oppdaterArenaStatus(sak.id, arenaMeldekortStatus)
                        logger.info { "Oppdaterte arena status for sak ${sak.id} til $arenaMeldekortStatus" }
                    } else {
                        logger.warn { "Arena status for sak ${sak.id} er ukjent" }
                    }
                }.onLeft {
                    logger.error(it) { "Feil under oppdatering av arena meldekort status for sak ${sak.id}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under oppdatering av arena meldekort status for saker" }
        }
    }
}
