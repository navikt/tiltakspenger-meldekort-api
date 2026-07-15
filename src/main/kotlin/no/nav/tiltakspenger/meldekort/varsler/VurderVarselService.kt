package no.nav.tiltakspenger.meldekort.varsler

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.jobb.JobbResultat
import no.nav.tiltakspenger.meldekort.varsler.SakVarselRepo
import no.nav.tiltakspenger.meldekort.varsler.VarselMeldekortRepo
import no.nav.tiltakspenger.meldekort.varsler.VarselRepo
import java.time.Clock

class VurderVarselService(
    private val sakVarselRepo: SakVarselRepo,
    private val varselMeldekortRepo: VarselMeldekortRepo,
    private val varselRepo: VarselRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    /** Returnerer [JobbResultat.IngenArbeid] når ingen saker var flagget for vurdering, slik at jobben kan melde fra om den hadde arbeid. */
    fun vurderVarsler(): JobbResultat {
        return Either.catch {
            val saker = sakVarselRepo.hentSakerSomSkalVurdereVarsel()
            if (saker.isNotEmpty()) {
                logger.debug { "Fant ${saker.size} saker hvor varsel skal vurderes: ${saker.map { it.sakId }}" }
            }
            saker.forEach { sakForVurdering -> vurderVarselForSak(sakForVurdering) }
            if (saker.isEmpty()) JobbResultat.IngenArbeid else JobbResultat.UtførteArbeid
        }.getOrElse {
            logger.error(it) { "Ukjent feil skjedde under vurdering av varsler" }
            JobbResultat.Feilet
        }
    }

    private fun vurderVarselForSak(sakForVurdering: SakForVarselvurdering) {
        val sakId = sakForVurdering.sakId
        Either.catch {
            vurderVarselForSak(
                sakId = sakId,
                saksnummer = sakForVurdering.saksnummer,
                fnr = sakForVurdering.fnr,
                sistFlaggetTidspunktVedLesing = sakForVurdering.sistFlaggetTidspunkt,
                clock = clock,
                sessionFactory = sessionFactory,
                hentVarsler = { varselRepo.hentVarslerForSakId(sakId) },
                hentFørsteKjedeSomManglerInnsending = { varselMeldekortRepo.hentFørsteKjedeSomManglerInnsending(sakId) },
                lagreVarsel = { varsel, sessionContext -> varselRepo.lagre(varsel, sessionContext = sessionContext) },
                markerVarselVurdert = { vurdertTidspunkt, sistFlagget, sessionContext ->
                    sakVarselRepo.markerVarselVurdert(sakId, vurdertTidspunkt, sistFlagget, sessionContext)
                },
            )
        }.onLeft {
            // OptimistiskLåsFeil fra markerVarselVurdert fanges her og logges.
            // Saken forblir flagget (skal_vurdere_varsel = true) siden transaksjonen ble rullet tilbake, og blir plukket opp på nytt i neste kjøring.
            logger.error(it) { "Feil under vurdering av varsel for sak $sakId" }
        }
    }
}
