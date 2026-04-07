package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo

class InaktiverVarslerService(
    private val meldekortRepo: MeldekortRepo,
    private val tmsVarselClient: TmsVarselClient,
) {
    private val log = KotlinLogging.logger { }

    fun inaktiverVarslerForMottatteMeldekort() {
        Either.catch {
            val meldekortSomSkalInaktiveres = meldekortRepo.henteMeldekortSomSkalInaktivereVarsel()
            log.debug { "Fant ${meldekortSomSkalInaktiveres.size} meldekort vi skal inaktivere varsler for." }

            meldekortSomSkalInaktiveres.forEach { meldekort ->
                val varselId = meldekort.varselId

                Either.catch {
                    tmsVarselClient.inaktiverVarsel(varselId)
                }.onLeft {
                    log.error(it) { "Feil under inaktivering (publisering) av varsel for meldekort ${meldekort.id} / varsel id $varselId" }
                }.onRight {
                    Either.catch {
                        val meldekortMedInaktivertVarsel = meldekort.inaktiverVarsel()
                        meldekortRepo.lagre(meldekortMedInaktivertVarsel)
                    }.onLeft {
                        log.error(it) { "Feil under lagring av inaktivert varsel for meldekort ${meldekort.id} / varsel id $varselId. Denne vil bli prøvd på nytt." }
                    }.onRight {
                        log.info { "Varsel $varselId inaktivert og lagret for meldekort ${meldekort.id}. Denne vil bli prøvd på nytt, men bør være idempotent på varslingteamet sin side." }
                    }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under inaktivering av varsler" }
        }
    }
}
