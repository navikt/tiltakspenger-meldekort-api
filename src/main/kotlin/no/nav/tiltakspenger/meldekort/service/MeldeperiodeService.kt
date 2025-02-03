package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.tilTomtBrukersMeldekort
import no.nav.tiltakspenger.meldekort.repository.BrukersMeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.routes.meldekort.tilMeldeperiode
import java.util.*

class MeldeperiodeService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
    private val tmsVarselClient: TmsVarselClient,
    private val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    fun lagreFraSaksbehandling(meldeperiodeDto: MeldeperiodeDTO): Either<FeilVedMottakAvMeldeperiode, Unit> {
        val meldeperiode = meldeperiodeDto.tilMeldeperiode().getOrElse {
            logger.error { "Ugyldig meldeperiode ${meldeperiodeDto.id} - ${it.message}" }
            return FeilVedMottakAvMeldeperiode.UgyldigMeldeperiode.left()
        }

        meldeperiodeRepo.hentForId(meldeperiode.id)?.also {
            logger.error { "Meldeperioden ${it.id} finnes allerede" }
            return FeilVedMottakAvMeldeperiode.MeldeperiodeFinnes.left()
        }

        val brukersMeldekort = if (meldeperiode.harRettIPerioden) meldeperiode.tilTomtBrukersMeldekort() else null

        Either.catch {
            sessionFactory.withTransactionContext { tx ->
                meldeperiodeRepo.lagre(meldeperiode, tx)
                brukersMeldekort?.also { brukersMeldekortRepo.lagre(it, tx) }
            }
        }.mapLeft {
            with("Lagring av meldeperiode feilet for ${meldeperiode.id}") {
                logger.error { this }
                sikkerlogg.error(it) { this }
            }
            return FeilVedMottakAvMeldeperiode.LagringFeilet.left()
        }

        logger.info { "Lagret meldeperiode ${meldeperiode.id}" }

        if (brukersMeldekort != null) {
            logger.info { "Lagret brukers meldekort ${brukersMeldekort.id}" }
            tmsVarselClient.sendVarselForNyttMeldekort(brukersMeldekort, eventId = UUID.randomUUID().toString())
        }

        return Unit.right()
    }


    fun hentMeldeperiodeForKjedeId(kjedeId: MeldeperiodeKjedeId): Meldeperiode? {
        TODO()
    }
}

sealed interface FeilVedMottakAvMeldeperiode {
    data object UgyldigMeldeperiode : FeilVedMottakAvMeldeperiode
    data object MeldeperiodeFinnes : FeilVedMottakAvMeldeperiode
    data object LagringFeilet : FeilVedMottakAvMeldeperiode
}
