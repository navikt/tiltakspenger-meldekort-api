package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.Clock
import java.time.LocalDateTime

class SendVarslerService(
    private val meldekortRepo: MeldekortRepo,
    private val tmsVarselClient: TmsVarselClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    fun sendVarselForMeldekort() {
        Either.catch {
            val meldekortUtenVarsel = meldekortRepo.hentMeldekortDetSkalVarslesFor()
            log.debug { "Fant ${meldekortUtenVarsel.size} meldekort det skal opprettes varsler for" }

            meldekortUtenVarsel.forEach { meldekort ->
                val varselId = meldekort.varselId

                Either.catch {
                    tmsVarselClient.sendVarselForNyttMeldekort(meldekort, varselId = varselId)
                }.onLeft {
                    log.error(it) { "Feil under sending av varsel for meldekort ${meldekort.id} / varsel id $varselId. Denne vil bli prøvd på nytt." }
                }.onRight { sendtVarselMetadata ->
                    Either.catch {
                        meldekortRepo.markerVarslet(meldekort.id, LocalDateTime.now(clock), sendtVarselMetadata)
                    }.onLeft {
                        log.error(it) { "Feil under lagring av marker varslet for meldekort ${meldekort.id} / varsel id $varselId. Denne vil bli publisert på nytt, men skal bli deduplisert av varslingsteamet." }
                    }.onRight {
                        log.info { "Varsel $varselId sendt og lagret for meldekort ${meldekort.id}" }
                    }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under sending eller lagring av varsler for meldekort" }
        }
    }
}
