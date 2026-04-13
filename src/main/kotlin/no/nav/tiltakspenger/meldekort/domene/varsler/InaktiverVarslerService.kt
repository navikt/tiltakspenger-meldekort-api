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
            val varslerSomSkalInaktiveres = varselRepo.hentVarslerSomSkalInaktiveres()
            log.debug { "Fant ${varslerSomSkalInaktiveres.size} varsler som skal inaktiveres. varselIder: ${varslerSomSkalInaktiveres.map { it.varselId }}" }

            varslerSomSkalInaktiveres.forEach { varsel ->
                Either.catch {
                    varsel.inaktiver(nå(clock)).fold(
                        ifLeft = { feil ->
                            log.warn { "Kunne ikke inaktivere varsel ${varsel.varselId}: ${feil.melding}" }
                        },
                        ifRight = { inaktivertVarsel ->
                            varselClient.inaktiverVarsel(inaktivertVarsel.varselId)
                            // Lagre inaktivering og re-flagg saken for vurdering i samme transaksjon.
                            // Re-flagging er nødvendig for at senere meldeperioder som fortsatt mangler
                            // innsending skal få opprettet et nytt varsel (invarianten "kun ett aktivt
                            // varsel" gjør at VurderVarselService ikke kunne opprette det nye varselet
                            // før varselet var inaktivert). Uten re-flagging ville hendelsen gå tapt
                            // inntil noe annet (ny meldeperiode/meldekort) trigget flaggingen.
                            sessionFactory.withTransactionContext { tx ->
                                varselRepo.lagre(varsel = inaktivertVarsel, sessionContext = tx)
                                sakVarselRepo.flaggForVarselvurdering(inaktivertVarsel.sakId, tx)
                            }
                            log.info { "Varsel ${inaktivertVarsel.varselId} inaktivert, lagret og sak ${inaktivertVarsel.sakId} re-flagget for vurdering" }
                        },
                    )
                }.onLeft {
                    log.error(it) { "Feil under inaktivering av varsel ${varsel.varselId}. Denne vil bli prøvd på nytt." }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under inaktivering av varsler" }
        }
    }
}
