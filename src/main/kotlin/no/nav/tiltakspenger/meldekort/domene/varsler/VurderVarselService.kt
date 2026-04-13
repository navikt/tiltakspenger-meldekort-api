package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.repository.SakVarselRepo
import no.nav.tiltakspenger.meldekort.repository.VarselMeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.VarselRepo
import java.time.Clock

class VurderVarselService(
    private val sakVarselRepo: SakVarselRepo,
    private val varselMeldekortRepo: VarselMeldekortRepo,
    private val varselRepo: VarselRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    fun vurderVarsler() {
        Either.catch {
            val saker = sakVarselRepo.hentSakerSomSkalVurdereVarsel()
            logger.debug { "Fant ${saker.size} saker som skal vurdere varsel: ${saker.map { it.sakId }}" }
            saker.forEach { sakForVurdering -> vurderVarselForSak(sakForVurdering) }
        }.onLeft {
            logger.error(it) { "Ukjent feil under vurdering av varsler" }
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
                hentKjederSomManglerInnsending = { varselMeldekortRepo.hentKjederSomManglerInnsending(sakId) },
                lagreVarsel = { varsel, sessionContext -> varselRepo.lagre(varsel, sessionContext = sessionContext) },
                markerVarselVurdert = { vurdertTidspunkt, sistFlagget, sessionContext ->
                    sakVarselRepo.markerVarselVurdert(sakId, vurdertTidspunkt, sistFlagget, sessionContext)
                },
            )
        }.onLeft {
            // OptimistiskLåsFeil fra markerVarselVurdert fanges her og logges. Saken forblir
            // flagget (skal_vurdere_varsel = true) siden transaksjonen ble rullet tilbake, og
            // blir plukket opp på nytt i neste kjøring.
            logger.error(it) { "Feil under vurdering av varsel for sak $sakId" }
        }
    }
}
