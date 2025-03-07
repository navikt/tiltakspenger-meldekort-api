package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.util.UUID

class SendVarslerService(
    private val meldekortRepo: MeldekortRepo,
    private val tmsVarselClient: TmsVarselClient,
) {
    private val log = KotlinLogging.logger { }

    fun sendVarselForMeldekort() {
        Either.catch {
            val meldkortUtenVarsel = meldekortRepo.hentDeSomIkkeHarBlittVarsletFor()
            log.info { "Fant ${meldkortUtenVarsel.size} meldekort det skal opprettes varsler for" }

            meldkortUtenVarsel.forEach { meldekort ->
                val varselId = UUID.randomUUID().toString()
                log.info { "Oppretter varsel $varselId for meldekort ${meldekort.id}" }
                meldekortRepo.oppdater(meldekort.copy(varselId = VarselId(varselId)))
                tmsVarselClient.sendVarselForNyttMeldekort(meldekort, varselId = varselId)
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under opprettelse av varsler for meldekort" }
        }
    }
}
