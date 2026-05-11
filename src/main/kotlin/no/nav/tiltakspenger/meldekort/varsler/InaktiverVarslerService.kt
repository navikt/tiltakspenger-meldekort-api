package no.nav.tiltakspenger.meldekort.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.varsler.SakVarselRepo
import no.nav.tiltakspenger.meldekort.varsler.VarselClient
import no.nav.tiltakspenger.meldekort.varsler.VarselRepo
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
                    val inaktiverteVarsler = varsler.pågåendeInaktiveringer
                        .filter { it.skalInaktiveresTidspunkt <= inaktiveringstidspunkt }
                        .map { varsler.inaktiver(it.varselId, inaktiveringstidspunkt).second }
                    sessionFactory.withTransactionContext { tx ->
                        // Lagre først – hvis optimistisk lås slår til kastes det her, og vi
                        // produserer aldri noe på Kafka.
                        inaktiverteVarsler.forEach { inaktivertVarsel ->
                            varselRepo.lagre(varsel = inaktivertVarsel, sessionContext = tx)
                        }
                        // Trigger en ny vurdering av saken.
                        sakVarselRepo.flaggForVarselvurdering(sakId, tx)
                        // Publiser inaktiveringen på Kafka inne i transaksjonen, etter lagre.
                        // Merk at vi også inaktiverer varsler som aldri har blitt sendt, hvis
                        // de har gått fra SkalAktiveres til SkalInaktiveres. Dette i tilfelle
                        // vi har aktivert et varsel uten å fått lagret det. Team Min Side
                        // forkaster bare disse siden de ikke kjenner igjen varselId.
                        inaktiverteVarsler.forEach { inaktivertVarsel ->
                            varselClient.inaktiverVarsel(inaktivertVarsel.varselId)
                        }
                    }
                    log.info { "${inaktiverteVarsler.size} varsler inaktivert, lagret og sak $sakId re-flagget for vurdering" }
                }.onLeft {
                    log.error(it) { "Feil under inaktivering av varsel for sak $sakId. Denne vil bli prøvd på nytt." }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under inaktivering av varsler" }
        }
    }
}
