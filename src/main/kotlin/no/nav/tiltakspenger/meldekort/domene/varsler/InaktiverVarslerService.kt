package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.clients.varsler.VarselClient
import no.nav.tiltakspenger.meldekort.repository.SakVarselRepo
import no.nav.tiltakspenger.meldekort.repository.VarselRepo
import java.time.Clock

class InaktiverVarslerService(
    private val varselRepo: VarselRepo,
    private val sakVarselRepo: SakVarselRepo,
    private val varselClient: VarselClient,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    fun inaktiverVarsler() {
        Either.catch {
            val sakerMedVarslerSomSkalInaktiveres = varselRepo.hentSakerMedVarslerSomSkalInaktiveres()
            log.debug { "Fant ${sakerMedVarslerSomSkalInaktiveres.size} saker med varsler som skal inaktiveres. sakIder: $sakerMedVarslerSomSkalInaktiveres" }

            sakerMedVarslerSomSkalInaktiveres.forEach { sakId ->
                Either.catch {
                    val inaktiveringstidspunkt = nå(clock)
                    val varsler = varselRepo.hentVarslerForSakId(sakId)
                    val varselSomSkalInaktiveres: Varsel.SkalInaktiveres = varsler.pågåendeInaktivering!!
                    val varselId = varselSomSkalInaktiveres.varselId
                    val inaktivertVarsel = varsler.inaktiver(varselId, inaktiveringstidspunkt).second
                    varselClient.inaktiverVarsel(varselId)
                    sessionFactory.withTransactionContext { tx ->
                        varselRepo.lagre(varsel = inaktivertVarsel, sessionContext = tx)
                        // Trigger en ny vurdering av saken.
                        sakVarselRepo.flaggForVarselvurdering(sakId, tx)
                    }
                    log.info { "Varsel $varselId inaktivert, lagret og sak $sakId re-flagget for vurdering" }
                }.onLeft {
                    log.error(it) { "Feil under inaktivering av varsel for sak $sakId. Denne vil bli prøvd på nytt." }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under inaktivering av varsler" }
        }
    }
}
