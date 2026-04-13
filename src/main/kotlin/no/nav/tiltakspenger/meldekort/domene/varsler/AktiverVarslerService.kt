package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.clients.varsler.VarselClient
import no.nav.tiltakspenger.meldekort.repository.VarselRepo
import java.time.Clock

class AktiverVarslerService(
    private val varselRepo: VarselRepo,
    private val varselClient: VarselClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    fun sendVarselForMeldekort() {
        Either.catch {
            val varslerSomSkalAktiveres = varselRepo.hentVarslerSomSkalAktiveres()
            log.debug { "Fant ${varslerSomSkalAktiveres.size} varsler som skal aktiveres. varselIder: ${varslerSomSkalAktiveres.map { it.varselId }}" }

            varslerSomSkalAktiveres.forEach { varsel ->
                Either.catch {
                    varsel.aktiver(nå(clock)).fold(
                        ifLeft = { feil ->
                            log.warn { "Kunne ikke aktivere varsel ${varsel.varselId}: ${feil.melding}" }
                        },
                        ifRight = { aktivertVarsel ->
                            val metadata = varselClient.sendVarsel(aktivertVarsel.varselId, aktivertVarsel.fnr)
                            varselRepo.lagre(
                                varsel = aktivertVarsel,
                                aktiveringsmetadata = metadata.jsonRequest,
                            )
                            log.info { "Varsel ${aktivertVarsel.varselId} aktivert og lagret" }
                        },
                    )
                }.onLeft {
                    log.error(it) { "Feil under aktivering av varsel ${varsel.varselId}. Denne vil bli prøvd på nytt." }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under aktivering av varsler" }
        }
    }
}
