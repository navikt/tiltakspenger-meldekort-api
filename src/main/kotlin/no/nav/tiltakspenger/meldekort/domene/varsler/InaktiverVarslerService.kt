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
            val mottatteMeldekort = meldekortRepo.hentMottatteEllerDeaktiverteSomDetVarslesFor()
            log.debug { "Fant ${mottatteMeldekort.size} mottatte meldekort som det varsles for" }

            mottatteMeldekort.forEach { meldekort ->
                meldekort.varselId?.let { varselId ->
                    log.info { "Inaktiverer varsel for meldekort med id ${meldekort.id} varselId=$varselId" }

                    Either.catch {
                        tmsVarselClient.inaktiverVarsel(varselId)
                        meldekortRepo.lagre(meldekort.inaktiverVarsel())
                        log.info { "Varsel inaktivert for meldekort med id ${meldekort.id} varselId=$varselId" }
                    }.onLeft {
                        log.error(it) { "Kunne ikke inaktivere varsel for meldekort med id ${meldekort.id} varselId=$varselId, prøver igjen neste jobbkjøring" }
                    }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under inaktivering av varsler" }
        }
    }
}
