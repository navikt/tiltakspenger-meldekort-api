package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo

class InaktiverVarslerService(
    private val meldekortRepo: MeldekortRepo,
    private val tmsVarselClient: TmsVarselClient,
) {
    private val log = KotlinLogging.logger { }

    fun inaktiverVarslerForMottatteMeldekort() {
        Either.catch {
            meldekortRepo.hentMottatteSomDetVarslesFor().forEach { meldekort ->
                meldekort.varselId?.let { varselId ->
                    log.info { "Inaktiverer varsel for meldekort med id ${meldekort.id} varselId=$varselId" }
                    val inaktivert = tmsVarselClient.inaktiverVarsel(varselId)
                    if (inaktivert) {
                        log.info { "Varsel inaktivert for meldekort med id ${meldekort.id} varselId=$varselId" }
                        meldekortRepo.lagre(meldekort.copy(varselId = null))
                    } else {
                        log.error { "Kunne ikke inaktivere varsel for meldekort med id ${meldekort.id} varselId=$varselId, prøver igjen neste jobbkjøring" }
                    }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under inaktivering av varsler" }
        }
    }
}
