package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo

class MeldeperiodeService(
    private val repo: MeldeperiodeRepo,
) {
    private val logger = KotlinLogging.logger {}

    fun lagreMeldeperiode(meldeperiode: Meldeperiode): Either<FeilVedLagringAvMeldekort, Unit> {
        return Either.catch {
            // Denne genereres i tiltakspenger-saksbehandling-api, så vi skal ikke behøve og validere den her.
            repo.lagre(meldeperiode)
            logger.info { "Lagret meldeperiode: ${meldeperiode.id}" }
        }.mapLeft {
            with("Lagring av meldeperiode feilet: ${meldeperiode.id}") {
                logger.error { this }
                sikkerlogg.error(it) { this }
            }
            FeilVedLagringAvMeldekort
        }
    }

    fun hentMeldeperiodeForId(id: String): Meldeperiode? {
        return repo.hentForId(id)
    }

    fun hentMeldeperiodeForKjedeId(kjedeId: String): Meldeperiode? {
        return repo.hentForId(kjedeId)
    }
}
