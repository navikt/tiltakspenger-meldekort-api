package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.clients.varsler.VarselClient
import no.nav.tiltakspenger.meldekort.repository.SakVarselRepo
import no.nav.tiltakspenger.meldekort.repository.VarselRepo
import java.time.Clock

class AktiverVarslerService(
    private val varselRepo: VarselRepo,
    private val sakVarselRepo: SakVarselRepo,
    private val varselClient: VarselClient,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    fun aktiverVarsler() {
        Either.catch {
            val sakerMedVarslerSomSkalAktiveres = varselRepo.hentSakerMedVarslerSomSkalAktiveres()
            log.debug { "Fant ${sakerMedVarslerSomSkalAktiveres.size} saker med varsler som skal aktiveres. sakIder: $sakerMedVarslerSomSkalAktiveres" }

            sakerMedVarslerSomSkalAktiveres.forEach { sakId ->
                Either.catch {
                    val aktiveringstidspunkt = nå(clock)
                    val varsler = varselRepo.hentVarslerForSakId(sakId)
                    val varselSomSkalAktiveres = varsler.pågåendeAktivering!!
                    val varselId = varselSomSkalAktiveres.varselId

                    val aktivertVarsel: Varsel.Aktiv = varsler.aktiver(varselId, aktiveringstidspunkt).second
                    // Bygg varselhendelsen utenfor transaksjonen – ren funksjon, ingen Kafka-kall.
                    val metadata = varselClient.byggVarsel(
                        varselId = varselId,
                        fnr = aktivertVarsel.fnr,
                        utsettSendingTil = aktivertVarsel.eksternAktiveringstidspunkt,
                    )
                    sessionFactory.withTransactionContext { tx ->
                        // Lagre først – hvis optimistisk lås slår til kastes det her, og vi
                        // produserer aldri noe på Kafka.
                        varselRepo.lagre(
                            varsel = aktivertVarsel,
                            aktiveringsmetadata = metadata.jsonRequest,
                            sessionContext = tx,
                        )
                        // Trigger en ny vurdering av saken.
                        sakVarselRepo.flaggForVarselvurdering(sakId, tx)
                        // Publiser på Kafka inne i transaksjonen, etter lagre. Hvis Kafka-
                        // produsenten kaster ruller vi tilbake både lagre og flagg, og jobben
                        // kjører på nytt. Hvis lagre lyktes men commit feiler etter Kafka-
                        // produksjonen vil Min side dedupliserer på varselId ved retry.
                        varselClient.sendVarsel(varselId = varselId, metadata = metadata)
                    }
                    log.info { "Varsel $varselId aktivert og lagret, sak $sakId re-flagget for vurdering" }
                }.onLeft {
                    log.error(it) { "Feil under aktivering av varsel for sak $sakId. Denne vil bli prøvd på nytt." }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under aktivering av varsler" }
        }
    }
}
