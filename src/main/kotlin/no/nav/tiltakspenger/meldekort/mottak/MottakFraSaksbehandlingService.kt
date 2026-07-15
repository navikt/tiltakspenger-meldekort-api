package no.nav.tiltakspenger.meldekort.mottak

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortRepo
import no.nav.tiltakspenger.meldekort.meldekort.tilOppdatertMeldekort
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.meldeperiode.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.sak.SakRepo
import no.nav.tiltakspenger.meldekort.varsler.SakVarselRepo

class MottakFraSaksbehandlingService(
    private val sakRepo: SakRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val meldekortRepo: MeldekortRepo,
    private val sakVarselRepo: SakVarselRepo,
    private val mottakRepo: MottakRepo,
    private val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    fun lagre(mottattSak: MottattSak): Either<FeilVedMottakAvSak, Unit> {
        val sakId = mottattSak.id
        logger.debug { "Mottok sak med id $sakId fra saksbehandling" }
        val eksisterendeSak = sakRepo.hent(sakId)

        if (eksisterendeSak != null && mottattSak.erLik(eksisterendeSak)) {
            logger.info { "Sak $sakId finnes allerede med samme data" }
            return FeilVedMottakAvSak.FinnesUtenDiff.left()
        }

        val meldeperioderForLagring = mottattSak.meldeperioder.filter { nyMeldeperiode ->
            val meldeperiodeId = nyMeldeperiode.id

            val eksisterendeMeldeperiode = meldeperiodeRepo.hentForId(meldeperiodeId) ?: return@filter true

            if (eksisterendeMeldeperiode.erLik(nyMeldeperiode)) {
                logger.debug { "Meldeperioden $meldeperiodeId finnes allerede med samme data" }
                return@filter false
            }

            logger.error { "Meldeperioden $meldeperiodeId finnes allerede med andre data - Eksisterende: $eksisterendeMeldeperiode - Ny: $nyMeldeperiode" }
            return FeilVedMottakAvSak.MeldeperiodeFinnesMedDiff.left()
        }

        val eksisterendeVedtakIder = eksisterendeSak?.meldekortvedtakIdListe?.toSet() ?: emptySet()
        val nyeVedtak = mottattSak.meldekortvedtak.filter { it.id !in eksisterendeVedtakIder }

        Either.catch {
            sessionFactory.withTransactionContext { tx ->
                Either.catch {
                    if (eksisterendeSak == null) {
                        logger.info { "Lagrer ny sak med id $sakId" }
                        mottakRepo.lagreSak(mottattSak, tx)
                    } else {
                        logger.info { "Oppdaterer sak med id $sakId" }
                        mottakRepo.oppdaterSak(mottattSak, tx)
                    }
                }.onLeft {
                    throw RuntimeException("Feil under lagring eller oppdatering av sak.", it)
                }
                // Vi flagger saken kun for ny varselsvurdering dersom vi har meldeperioder.
                // Team Min Side bør støtte at en bruker bytter fødselsnummer.
                // På sikt kan vi vurdere om vi ønsker å varsle brukere som kan melde helg, men det er forsvinnende få slike saker og de bør få en mer direkte beskjed i f.eks. Modia.
                meldeperioderForLagring.forEach { meldeperiode ->
                    val meldeperiodeId = meldeperiode.id
                    Either.catch {
                        lagreMeldeperiode(meldeperiode, tx)
                    }.onLeft {
                        throw RuntimeException("Feil under lagring av meldeperiode $meldeperiodeId", it)
                    }
                }
                nyeVedtak.forEach { vedtak ->
                    Either.catch {
                        // Logger positiv/negativ i repoet.
                        mottakRepo.lagreMeldekortvedtak(vedtak, tx)
                    }.onLeft {
                        throw RuntimeException("Feil under lagring av meldekortvedtak ${vedtak.id}", it)
                    }
                }
            }
        }.onLeft {
            logger.error(it) { "Feil under lagring av sak eller meldeperioder for $sakId. finnesEksisterendeSak: ${eksisterendeSak != null}" }
            return FeilVedMottakAvSak.LagringFeilet.left()
        }

        logger.info { "Lagret sak $sakId med ${meldeperioderForLagring.size} nye meldeperioder og ${nyeVedtak.size} nye meldekortvedtak" }

        return Unit.right()
    }

    private fun lagreMeldeperiode(meldeperiode: Meldeperiode, tx: TransactionContext) {
        val eksisterendeMeldekort = meldekortRepo.hentMeldekortForKjedeId(meldeperiode.kjedeId, meldeperiode.fnr, tx)
        val aktiveMeldekort = eksisterendeMeldekort.filter { it.deaktivert == null && it.mottatt == null }

        val nyttMeldekort = lagMeldekort(meldeperiode, eksisterendeMeldekort.lastOrNull())

        mottakRepo.lagreMeldeperiode(meldeperiode, tx)
        logger.info { "Lagret meldeperiode ${meldeperiode.id}" }
        aktiveMeldekort.forEach {
            meldekortRepo.deaktiver(it.id, tx)
            logger.info { "Deaktiverte meldekortet ${it.id}" }
        }

        nyttMeldekort?.also {
            meldekortRepo.lagre(it, tx)
            logger.info { "Lagret brukers meldekort ${nyttMeldekort.id}" }
        }

        // Håndter varsler: flagg saken for vurdering av den asynkrone jobben
        sakVarselRepo.flaggForVarselvurdering(meldeperiode.sakId, tx)
    }

    private fun lagMeldekort(
        meldeperiode: Meldeperiode,
        eksisterendeMeldekort: BrukersMeldekort?,
    ): BrukersMeldekort? {
        return meldeperiode.tilOppdatertMeldekort(eksisterendeMeldekort)
    }
}

enum class FeilVedMottakAvSak {
    FinnesUtenDiff,
    LagringFeilet,
    MeldeperiodeFinnesMedDiff,
}
