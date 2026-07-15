package no.nav.tiltakspenger.meldekort.varsler

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.jobb.JobbResultat
import java.time.Clock

class AktiverVarslerService(
    private val varselRepo: VarselRepo,
    private val sakVarselRepo: SakVarselRepo,
    private val varselClient: VarselClient,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    /** Returnerer [JobbResultat.IngenArbeid] når ingen saker hadde varsler som skulle aktiveres, slik at jobben kan melde fra om den hadde arbeid. */
    fun aktiverVarsler(): JobbResultat {
        return Either.catch {
            val sakerMedVarslerSomSkalAktiveres = varselRepo.hentSakerMedVarslerSomSkalAktiveres()
            if (sakerMedVarslerSomSkalAktiveres.isNotEmpty()) {
                log.debug { "Fant ${sakerMedVarslerSomSkalAktiveres.size} saker med varsler som skal aktiveres. sakIder: $sakerMedVarslerSomSkalAktiveres" }
            }

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
                        // Lagre først – hvis optimistisk lås slår til, kastes det her, og vi produserer aldri noe på Kafka.
                        varselRepo.lagre(
                            varsel = aktivertVarsel,
                            aktiveringsmetadata = metadata.jsonRequest,
                            sessionContext = tx,
                        )
                        // Trigger en ny vurdering av saken.
                        sakVarselRepo.flaggForVarselvurdering(sakId, tx)
                        // Publiser på Kafka inne i transaksjonen, etter lagringen.
                        // Hvis Kafka-produsenten kaster, ruller vi tilbake både lagringen og flaggingen, og jobben kjører på nytt.
                        // Hvis lagringen lyktes, men commit feiler etter Kafka-produksjonen, vil Min side deduplisere på varselId ved retry.
                        varselClient.sendVarsel(varselId = varselId, metadata = metadata)
                    }
                    log.info { "Varsel $varselId aktivert og lagret, sak $sakId re-flagget for vurdering" }
                }.onLeft {
                    log.error(it) { "Feil under aktivering av varsel for sak $sakId. Denne vil bli prøvd på nytt." }
                }
            }
            if (sakerMedVarslerSomSkalAktiveres.isEmpty()) JobbResultat.IngenArbeid else JobbResultat.UtførteArbeid
        }.getOrElse {
            log.error(it) { "Ukjent feil skjedde under aktivering av varsler" }
            JobbResultat.Feilet
        }
    }
}
