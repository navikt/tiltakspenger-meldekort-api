package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.tilMeldeperiode
import no.nav.tiltakspenger.meldekort.domene.tilTomtMeldekort
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo

class MeldeperiodeService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val meldekortRepo: MeldekortRepo,
    private val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    fun lagreFraSaksbehandling(meldeperiodeDto: MeldeperiodeDTO): Either<FeilVedMottakAvMeldeperiode, Unit> {
        val meldeperiode = meldeperiodeDto.tilMeldeperiode().getOrElse {
            logger.error { "Ugyldig meldeperiode ${meldeperiodeDto.id} - ${it.message}" }
            return FeilVedMottakAvMeldeperiode.UgyldigMeldeperiode.left()
        }

        meldeperiodeRepo.hentForId(meldeperiode.id)?.also {
            if (it == meldeperiode) {
                logger.info { "Meldeperioden ${it.id} finnes allerede" }
                return FeilVedMottakAvMeldeperiode.MeldeperiodeFinnesUtenDiff.left()
            }

            logger.error { "Meldeperioden ${it.id} finnes allerede med andre data!" }
            return FeilVedMottakAvMeldeperiode.MeldeperiodeFinnesMedDiff.left()
        }

        val meldekort = if (meldeperiode.harRettIPerioden) meldeperiode.tilTomtMeldekort() else null

        Either.catch {
            sessionFactory.withTransactionContext { tx ->
                meldeperiodeRepo.lagre(meldeperiode, tx)
                logger.info { "Lagret meldeperiode ${meldeperiode.id}" }
                meldekort?.also {
                    meldekortRepo.lagre(it, tx)
                    logger.info { "Lagret brukers meldekort ${meldekort.id}" }
                }
            }
        }.mapLeft {
            with("Lagring av meldeperiode feilet for ${meldeperiode.id}") {
                logger.error { this }
                sikkerlogg.error(it) { this }
            }
            return FeilVedMottakAvMeldeperiode.LagringFeilet.left()
        }
        return Unit.right()
    }
}

sealed interface FeilVedMottakAvMeldeperiode {
    data object UgyldigMeldeperiode : FeilVedMottakAvMeldeperiode
    data object MeldeperiodeFinnesUtenDiff : FeilVedMottakAvMeldeperiode
    data object MeldeperiodeFinnesMedDiff : FeilVedMottakAvMeldeperiode
    data object LagringFeilet : FeilVedMottakAvMeldeperiode
}
