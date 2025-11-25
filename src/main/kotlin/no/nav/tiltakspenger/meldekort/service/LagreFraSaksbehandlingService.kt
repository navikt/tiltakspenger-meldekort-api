package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.tilOppdatertMeldekort
import no.nav.tiltakspenger.meldekort.domene.tilSak
import no.nav.tiltakspenger.meldekort.domene.tilTomtMeldekort
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.repository.SakRepo

class LagreFraSaksbehandlingService(
    private val sakRepo: SakRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val meldekortRepo: MeldekortRepo,
    private val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    fun lagre(sakDTO: SakTilMeldekortApiDTO): Either<FeilVedMottakAvSak, Unit> {
        logger.debug { "Mottok sak med id ${sakDTO.sakId} fra saksbehandling" }

        val sak = Either.catch { sakDTO.tilSak() }.getOrElse {
            logger.error { "Kunne ikke opprette sak fra saksbehandling - $it" }
            return FeilVedMottakAvSak.OpprettSakFeilet.left()
        }

        val sakId = sak.id
        val eksisterendeSak = sakRepo.hent(sakId)

        if (eksisterendeSak != null && eksisterendeSak.erLik(sak)) {
            logger.info { "Sak $sakId finnes allerede med samme data" }
            return FeilVedMottakAvSak.FinnesUtenDiff.left()
        }

        val meldeperioderForLagring = sak.meldeperioder.filter { nyMeldeperiode ->
            val meldeperiodeId = nyMeldeperiode.id

            val eksisterendeMeldeperiode = meldeperiodeRepo.hentForId(meldeperiodeId)

            if (eksisterendeMeldeperiode == null) {
                return@filter true
            }

            if (eksisterendeMeldeperiode.erLik(nyMeldeperiode)) {
                logger.debug { "Meldeperioden $meldeperiodeId finnes allerede med samme data" }
                return@filter false
            }

            logger.error { "Meldeperioden $meldeperiodeId finnes allerede med andre data - Eksisterende: $eksisterendeMeldeperiode - Ny: $nyMeldeperiode" }
            return FeilVedMottakAvSak.MeldeperiodeFinnesMedDiff.left()
        }

        Either.catch {
            sessionFactory.withTransactionContext { tx ->
                Either.catch {
                    if (eksisterendeSak == null) {
                        logger.info { "Lagrer sak med id $sakId" }
                        sakRepo.lagre(sak, tx)
                    } else {
                        logger.info { "Oppdaterer sak med id $sakId" }
                        sakRepo.oppdater(sak, tx)
                    }
                }.onLeft {
                    logger.error { "Feil under lagring av sak $sakId" }
                    throw it
                }

                meldeperioderForLagring.forEach { meldeperiode ->
                    val meldeperiodeId = meldeperiode.id
                    Either.catch {
                        lagreMeldeperiode(meldeperiode, tx)
                    }.onLeft {
                        logger.error(it) { "Feil under lagring av meldeperiode $meldeperiodeId for sak $sakId" }
                        throw it
                    }
                }
            }
        }.onLeft {
            logger.error { "Feil under lagring av sak $sakId" }
            return FeilVedMottakAvSak.LagringFeilet.left()
        }

        logger.info { "Lagret sak $sakId med ${meldeperioderForLagring.size} nye meldeperioder" }

        return Unit.right()
    }

    private fun lagreMeldeperiode(meldeperiode: Meldeperiode, tx: SessionContext) {
        val eksisterendeMeldekort = meldekortRepo.hentMeldekortForKjedeId(meldeperiode.kjedeId, meldeperiode.fnr, tx)
        val aktiveMeldekort = eksisterendeMeldekort.filter { it.deaktivert == null && it.mottatt == null }

        val nyttMeldekort = lagMeldekort(meldeperiode, eksisterendeMeldekort.lastOrNull())

        meldeperiodeRepo.lagre(meldeperiode, tx)
        logger.info { "Lagret meldeperiode ${meldeperiode.id}" }

        aktiveMeldekort.forEach {
            /** Deaktiverer varselet for tidligere meldekort dersom det ikke har blitt generert et nytt meldekort,
             *  dvs det ikke er rett til tp i den nye versjonen av meldeperioden.
             *
             *  Dersom nytt meldekort ble opprettet skal varselet fortsatt være aktivt for dette
             * */
            meldekortRepo.deaktiver(it.id, deaktiverVarsel = nyttMeldekort == null, tx)
            logger.info { "Deaktiverte meldekortet ${it.id}" }
        }

        nyttMeldekort?.also {
            meldekortRepo.lagre(it, tx)
            logger.info { "Lagret brukers meldekort ${nyttMeldekort.id}" }
        }
    }

    private fun lagMeldekort(
        meldeperiode: Meldeperiode,
        eksisterendeMeldekort: Meldekort?,
    ): Meldekort? {
        if (!meldeperiode.minstEnDagGirRettIPerioden) {
            return null
        }

        if (eksisterendeMeldekort == null) {
            return meldeperiode.tilTomtMeldekort()
        }

        // Ikke lag et nytt meldekort dersom meldekortet allerede var mottatt
        // Bruker må selv opprette en korrigering dersom det er endringer som påvirker allerede innsendte meldekort
        if (eksisterendeMeldekort.erInnsendt) {
            return null
        }

        return meldeperiode.tilOppdatertMeldekort(eksisterendeMeldekort)
    }
}

enum class FeilVedMottakAvSak {
    FinnesUtenDiff,
    LagringFeilet,
    MeldeperiodeFinnesMedDiff,
    OpprettSakFeilet,
}
