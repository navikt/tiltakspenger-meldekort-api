package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo

class SendVarslerService(
    private val meldekortRepo: MeldekortRepo,
    private val tmsVarselClient: TmsVarselClient,
) {
    private val log = KotlinLogging.logger { }

    fun sendVarselForMeldekort() {
        Either.catch {
            val meldekortUtenVarsel = meldekortRepo.hentDeSomIkkeHarBlittVarsletFor()
            log.debug { "Fant ${meldekortUtenVarsel.size} meldekort det skal opprettes varsler for" }

            meldekortUtenVarsel.forEach { meldekort ->
                val varselId = VarselId.random()
                log.info { "Oppretter varsel $varselId for meldekort ${meldekort.id}" }

                Either.catch {
                    tmsVarselClient.sendVarselForNyttMeldekort(meldekort, varselId = varselId)
                    meldekortRepo.oppdater(meldekort.copy(varselId = varselId))
                }.onLeft {
                    log.error(it) { "Feil under sending av varsel for meldekort ${meldekort.id} / varsel id $varselId" }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under opprettelse av varsler for meldekort" }
        }
    }
}
