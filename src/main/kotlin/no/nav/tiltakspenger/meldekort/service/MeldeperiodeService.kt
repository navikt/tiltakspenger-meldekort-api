package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.tilMeldeperiode
import no.nav.tiltakspenger.meldekort.domene.tilOppdatertMeldekort
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

        val eksisterendeMeldekort = meldekortRepo.hentMeldekortForKjedeId(meldeperiode.kjedeId, meldeperiode.fnr)
        val aktiveMeldekort = eksisterendeMeldekort.filter { it.deaktivert == null && it.mottatt == null }

        val nyttMeldekort = lagMeldekort(meldeperiode, eksisterendeMeldekort.lastOrNull())

        Either.catch {
            sessionFactory.withTransactionContext { tx ->
                meldeperiodeRepo.lagre(meldeperiode, tx)
                logger.info { "Lagret meldeperiode ${meldeperiode.id}" }
                aktiveMeldekort.forEach {
                    /** Deaktiverer varselet for tidligere meldekort dersom det ikke har blitt generert et nytt meldekort,
                     *  dvs det ikke er rett til tp i den nye versjonen av meldeperioden.
                     *
                     *  Dersom nytt meldekort ble opprettet skal varselet fortsatt være aktivt for dette
                     * */
                    meldekortRepo.deaktiver(it.id, deaktiverVarsel = nyttMeldekort == null)
                    logger.info { "Deaktiverte meldekortet ${it.id}" }
                }
                nyttMeldekort?.also {
                    meldekortRepo.opprett(it, tx)
                    logger.info { "Lagret brukers meldekort ${nyttMeldekort.id}" }
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

    private fun lagMeldekort(
        meldeperiode: Meldeperiode,
        eksisterendeMeldekort: Meldekort?,
    ): Meldekort? {
        if (!meldeperiode.harRettIPerioden) {
            return null
        }

        return eksisterendeMeldekort
            ?.let { meldeperiode.tilOppdatertMeldekort(it) }
            ?: meldeperiode.tilTomtMeldekort()
    }
}

sealed interface FeilVedMottakAvMeldeperiode {
    data object UgyldigMeldeperiode : FeilVedMottakAvMeldeperiode
    data object MeldeperiodeFinnesUtenDiff : FeilVedMottakAvMeldeperiode
    data object MeldeperiodeFinnesMedDiff : FeilVedMottakAvMeldeperiode
    data object LagringFeilet : FeilVedMottakAvMeldeperiode
}
