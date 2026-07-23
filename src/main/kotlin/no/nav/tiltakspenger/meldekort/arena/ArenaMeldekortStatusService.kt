package no.nav.tiltakspenger.meldekort.arena

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.meldekort.jobb.JobbResultat
import no.nav.tiltakspenger.meldekort.sak.SakRepo

class ArenaMeldekortStatusService(
    private val arenaMeldekortClient: ArenaMeldekortClient,
    private val sakRepo: SakRepo,
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentArenaMeldekortStatus(fnr: Fnr): ArenaMeldekortStatus {
        arenaMeldekortClient.hentMeldekort(fnr).onLeft {
            it.loggFeil(logger, "henting av meldekort fra Arena (meldekortservice)", "Bruker-oppslag; fnr ligger i request i sikkerlogg", sikkerlogg)
            return ArenaMeldekortStatus.UKJENT
        }.onRight {
            if (it == null) {
                return ArenaMeldekortStatus.HAR_IKKE_MELDEKORT
            } else if (it.harTiltakspengerMeldekort()) {
                return ArenaMeldekortStatus.HAR_MELDEKORT
            }
        }

        val historiskeMeldekort = arenaMeldekortClient.hentHistoriskeMeldekort(fnr).getOrElse {
            it.loggFeil(logger, "henting av historiske meldekort fra Arena (meldekortservice)", "Bruker-oppslag; fnr ligger i request i sikkerlogg", sikkerlogg)
            return ArenaMeldekortStatus.UKJENT
        }

        return if (historiskeMeldekort?.harTiltakspengerMeldekort() != true) {
            ArenaMeldekortStatus.HAR_IKKE_MELDEKORT
        } else {
            ArenaMeldekortStatus.HAR_MELDEKORT
        }
    }

    /** Returnerer [JobbResultat.IngenArbeid] når ingen saker manglet arena-status, slik at jobben kan melde fra om den hadde arbeid. */
    suspend fun oppdaterArenaMeldekortStatusForSaker(): JobbResultat {
        return Either.catch {
            // Saker uten meldeperioder og meldekortvedtak.
            val saker = sakRepo.hentSakerUtenArenaStatus()

            if (saker.isNotEmpty()) {
                logger.debug { "Fant ${saker.size} saker med ukjent Arena-status" }
            }

            saker.forEach { sak ->
                Either.catch {
                    val arenaMeldekortStatus = hentArenaMeldekortStatus(sak.fnr)
                    if (arenaMeldekortStatus != ArenaMeldekortStatus.UKJENT) {
                        sakRepo.oppdaterArenaStatus(sak.id, arenaMeldekortStatus)
                        logger.info { "Oppdaterte Arena-status for sak ${sak.id} til $arenaMeldekortStatus" }
                    } else {
                        logger.warn { "Arena-status for sak ${sak.id} er ukjent" }
                    }
                }.onLeft {
                    logger.warn(it) { "Feil under oppdatering av Arena-meldekortstatus for sak ${sak.id}" }
                }
            }
            if (saker.isEmpty()) JobbResultat.IngenArbeid else JobbResultat.UtførteArbeid
        }.getOrElse {
            logger.error(it) { "Ukjent feil skjedde under oppdatering av Arena-meldekortstatus for saker" }
            JobbResultat.Feilet
        }
    }
}
