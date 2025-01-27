package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.tilTomtBrukersMeldekort
import no.nav.tiltakspenger.meldekort.repository.BrukersMeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.routes.meldekort.tilMeldeperiode

class MeldeperiodeService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
    private val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    fun lagreFraSaksbehandling(meldeperiodeDto: MeldeperiodeDTO): Either<FeilVedMottakAvMeldeperiode, Unit> {
        val meldeperiode = meldeperiodeDto.tilMeldeperiode().getOrElse {
            return FeilVedMottakAvMeldeperiode.UgyldigMeldeperiode.left()
        }

        meldeperiodeRepo.hentForId(meldeperiode.id)?.also {
            return FeilVedMottakAvMeldeperiode.MeldeperiodeFinnes.left()
        }

        val brukersMeldekort = meldeperiode.tilTomtBrukersMeldekort()

        return Either.catch {
            sessionFactory.withTransactionContext { tx ->
                meldeperiodeRepo.lagre(meldeperiode, tx)
                brukersMeldekortRepo.lagre(brukersMeldekort, tx)
            }
            logger.info { "Lagret meldeperiode: ${meldeperiode.id} - Opprettet brukers meldekort: ${brukersMeldekort.id}" }
        }.mapLeft {
            with("Lagring av meldeperiode feilet for ${meldeperiode.id}") {
                logger.error { this }
                sikkerlogg.error(it) { this }
            }
            FeilVedMottakAvMeldeperiode.LagringFeilet
        }
    }

    fun hentMeldeperiodeForKjedeId(kjedeId: String): Meldeperiode? {
        return meldeperiodeRepo.hentForId(kjedeId)
    }
}

sealed interface FeilVedMottakAvMeldeperiode {
    data object UgyldigMeldeperiode : FeilVedMottakAvMeldeperiode
    data object MeldeperiodeFinnes : FeilVedMottakAvMeldeperiode
    data object LagringFeilet : FeilVedMottakAvMeldeperiode
}
