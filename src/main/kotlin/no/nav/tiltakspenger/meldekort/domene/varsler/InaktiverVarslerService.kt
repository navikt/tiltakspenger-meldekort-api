package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.clients.varsler.VarselClient
import no.nav.tiltakspenger.meldekort.repository.VarselRepo
import java.time.Clock

class InaktiverVarslerService(
    private val varselRepo: VarselRepo,
    private val varselClient: VarselClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    fun inaktiverVarsler() {
        Either.catch {
            val varslerSomSkalInaktiveres = varselRepo.hentVarslerSomSkalInaktiveres()
            log.debug { "Fant ${varslerSomSkalInaktiveres.size} varsler som skal inaktiveres. varselIder: ${varslerSomSkalInaktiveres.map { it.varselId }}" }

            varslerSomSkalInaktiveres.forEach { varsel ->
                Either.catch {
                    varsel.inaktiver(nå(clock)).fold(
                        ifLeft = { feil ->
                            log.warn { "Kunne ikke inaktivere varsel ${varsel.varselId}: ${feil.melding}" }
                        },
                        ifRight = { inaktivertVarsel ->
                            varselClient.inaktiverVarsel(inaktivertVarsel.varselId)
                            varselRepo.lagre(varsel = inaktivertVarsel)
                            log.info { "Varsel ${inaktivertVarsel.varselId} inaktivert og lagret" }
                        },
                    )
                }.onLeft {
                    log.error(it) { "Feil under inaktivering av varsel ${varsel.varselId}. Denne vil bli prøvd på nytt." }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under inaktivering av varsler" }
        }
    }
}
