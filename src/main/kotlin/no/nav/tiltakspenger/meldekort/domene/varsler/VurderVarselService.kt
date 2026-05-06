package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.clients.varsler.VarselClient
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.repository.BeskjedVarselRepo
import no.nav.tiltakspenger.meldekort.repository.SakVarselRepo
import no.nav.tiltakspenger.meldekort.repository.VarselMeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.VarselRepo
import java.time.Clock

class VurderVarselService(
    private val sakVarselRepo: SakVarselRepo,
    private val varselMeldekortRepo: VarselMeldekortRepo,
    private val varselRepo: VarselRepo,
    private val beskjedVarselRepo: BeskjedVarselRepo,
    private val varselClient: VarselClient,
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
            sendBeskjedHvisNødvendig(sakForVurdering)
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

    private fun sendBeskjedHvisNødvendig(sakForVurdering: SakForVarselvurdering) {
        val meldeperioder = varselMeldekortRepo.hentMeldeperioderSomSkalHaBeskjed(sakForVurdering.sakId)
        if (meldeperioder.isEmpty()) {
            return
        }
        val beskjedVarsel = BeskjedVarsel(
            varselId = VarselId.random(),
            sakId = sakForVurdering.sakId,
            saksnummer = sakForVurdering.saksnummer,
            fnr = sakForVurdering.fnr,
            meldeperioder = meldeperioder,
            opprettet = nå(clock),
        )
        val metadata = varselClient.byggMeldeperiodeEndretBeskjed(
            varselId = beskjedVarsel.varselId,
            fnr = beskjedVarsel.fnr,
            utsettSendingTil = null,
        )
        sessionFactory.withTransactionContext { tx ->
            beskjedVarselRepo.lagre(
                beskjedVarsel = beskjedVarsel,
                sendingsmetadata = metadata.jsonRequest,
                sessionContext = tx,
            )
            varselClient.sendVarsel(varselId = beskjedVarsel.varselId, metadata = metadata)
        }
        logger.info { "Sendte beskjed ${beskjedVarsel.varselId} for ${meldeperioder.size} endrede meldeperioder i sak ${sakForVurdering.sakId}" }
    }
}
