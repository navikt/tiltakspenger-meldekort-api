package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Orkestrerer alle varseljobbene i riktig rekkefølge.
 *
 * Rekkefølgen er viktig:
 *  1. [VurderVarselService.vurderVarsler]       – Oppretter/oppdaterer planlagte varsler basert
 *                                                  på gjeldende tilstand (kjeder som mangler
 *                                                  innsending, avbrutte meldeperioder, m.m.).
 *  2. [AktiverVarslerService.sendVarselForMeldekort] – Aktiverer (sender) varsler som nå er modne
 *                                                  (SkalAktiveres → Aktiv).
 *  3. [InaktiverVarslerService.inaktiverVarsler]  – Inaktiverer varsler som er planlagt inaktivert
 *                                                  (SkalInaktiveres → Inaktivert).
 *
 * Ved å samle kallene i én orkestrator sikrer vi at Application.kt (og tester) alltid kjører
 * jobbene i samme rekkefølge, og reduserer duplisering.
 */
class VarselJobber(
    private val vurderVarselService: VurderVarselService,
    private val aktiverVarslerService: AktiverVarslerService,
    private val inaktiverVarslerService: InaktiverVarslerService,
) {
    private val log = KotlinLogging.logger {}

    fun kjørAlle() {
        Either.catch { vurderVarselService.vurderVarsler() }
            .onLeft { log.error(it) { "Ukjent feil under vurderVarsler()" } }
        Either.catch { aktiverVarslerService.sendVarselForMeldekort() }
            .onLeft { log.error(it) { "Ukjent feil under sendVarselForMeldekort()" } }
        Either.catch { inaktiverVarslerService.inaktiverVarsler() }
            .onLeft { log.error(it) { "Ukjent feil under inaktiverVarsler()" } }
    }
}
