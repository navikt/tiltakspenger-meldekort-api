package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.meldekort.domene.tilSak
import no.nav.tiltakspenger.meldekort.repository.SakRepo

class SakService(
    private val sakRepo: SakRepo,
    private val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    fun hentSak(fnr: Fnr): Sak? {
        return sakRepo.hent(fnr)
    }

    fun lagreFraSaksbehandling(sakDTO: SakDTO): Either<FeilVedMottakAvSak, Unit> {
        val sak = sakDTO.tilSak()
        val sakId = sak.id

        sakRepo.hent(sakId)?.also { eksisterendeSak ->
            val nySakMedArenaStatus = sak.copy(arenaMeldekortStatus = eksisterendeSak.arenaMeldekortStatus)

            if (nySakMedArenaStatus == eksisterendeSak) {
                logger.info { "Sak $sakId er allerede lagret" }
                return FeilVedMottakAvSak.FinnesUtenDiff.left()
            }

            Either.catch {
                sessionFactory.withTransactionContext { tx ->
                    sakRepo.oppdater(sak, tx)
                    logger.info { "Oppdaterte sak $sakId" }
                }
                return Unit.right()
            }.mapLeft {
                with("Oppdatering av sak feilet for $sakId") {
                    logger.error { this }
                    sikkerlogg.error(it) { this }
                }
                return FeilVedMottakAvSak.OppdateringFeilet.left()
            }
        }

        Either.catch {
            sessionFactory.withTransactionContext { tx ->
                sakRepo.lagre(sak, tx)
                logger.info { "Lagret sak $sakId" }
            }
        }.mapLeft {
            with("Lagring av sak feilet for $sakId") {
                logger.error { this }
                sikkerlogg.error(it) { this }
            }
            return FeilVedMottakAvSak.LagringFeilet.left()
        }

        return Unit.right()
    }
}

sealed interface FeilVedMottakAvSak {
    data object FinnesUtenDiff : FeilVedMottakAvSak
    data object LagringFeilet : FeilVedMottakAvSak
    data object OppdateringFeilet : FeilVedMottakAvSak
}

// TODO: flytt til libs
data class SakDTO(
    val fnr: String,
    val sakId: String,
    val saksnummer: String,
    val innvilgelsesperioder: List<PeriodeDTO>,
)
