package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.domene.tilMeldeperiode
import no.nav.tiltakspenger.meldekort.domene.tilTomtMeldekort
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import java.util.UUID

class MeldeperiodeService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val meldekortRepo: MeldekortRepo,
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

        val meldekort = if (meldeperiode.harRettIPerioden) meldeperiode.tilTomtMeldekort() else null

        Either.catch {
            sessionFactory.withTransactionContext { tx ->
                meldeperiodeRepo.lagre(meldeperiode, tx)
                meldekort?.also { meldekortRepo.lagre(it, tx) }
            }
        }.mapLeft {
            with("Lagring av meldeperiode feilet for ${meldeperiode.id}") {
                logger.error { this }
                sikkerlogg.error(it) { this }
            }
            return FeilVedMottakAvMeldeperiode.LagringFeilet.left()
        }

        logger.info { "Lagret meldeperiode ${meldeperiode.id}" }

        if (meldekort != null) {
            logger.info { "Lagret brukers meldekort ${meldekort.id}" }
            val varselId = UUID.randomUUID().toString()
            logger.info { "Oppretter varsel $varselId for meldekort ${meldekort.id}" }
            tmsVarselClient.sendVarselForNyttMeldekort(meldekort, varselId = varselId)
            meldekortRepo.oppdater(meldekort.copy(varselId = VarselId(varselId)))
        }

        return Unit.right()
    }
}

sealed interface FeilVedMottakAvMeldeperiode {
    data object UgyldigMeldeperiode : FeilVedMottakAvMeldeperiode
    data object MeldeperiodeFinnes : FeilVedMottakAvMeldeperiode
    data object LagringFeilet : FeilVedMottakAvMeldeperiode
}
