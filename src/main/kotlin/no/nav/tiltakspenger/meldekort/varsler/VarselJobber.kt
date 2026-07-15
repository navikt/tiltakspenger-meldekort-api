package no.nav.tiltakspenger.meldekort.varsler

import no.nav.tiltakspenger.meldekort.jobb.JobbResultat
import no.nav.tiltakspenger.meldekort.jobb.tilSamletResultat

/**
 * Orkestrerer alle varseljobbene i riktig rekkefølge.
 *
 * Rekkefølgen er viktig:
 *  1. [VurderVarselService.vurderVarsler] – Oppretter/oppdaterer planlagte varsler basert på gjeldende tilstand (kjeder hvor vi aldri har mottatt meldekort, avbrutte meldeperioder, m.m.).
 *  2. [AktiverVarslerService.aktiverVarsler] – Aktiverer (sender) varsler som nå er modne (SkalAktiveres → Aktiv).
 *  3. [InaktiverVarslerService.inaktiverVarsler] – Inaktiverer varsler som er planlagt inaktivert (SkalInaktiveres → Inaktivert).
 *
 * Ved å samle kallene i én orkestrator sikrer vi at Application.kt (og tester) alltid kjører jobbene i samme rekkefølge, og reduserer duplisering.
 */
class VarselJobber(
    private val vurderVarselService: VurderVarselService,
    private val aktiverVarslerService: AktiverVarslerService,
    private val inaktiverVarslerService: InaktiverVarslerService,
) {
    /**
     * Returnerer det samlede [JobbResultat] for de tre jobbene, slik at jobben kan melde fra om den hadde arbeid eller feilet.
     * Hver service fanger og logger sine egne feil og melder [JobbResultat.Feilet]; skulle noe likevel kaste, fanger og logger jobb-executoren det.
     */
    fun kjørAlle(): JobbResultat {
        return listOf(
            vurderVarselService.vurderVarsler(),
            aktiverVarslerService.aktiverVarsler(),
            inaktiverVarslerService.inaktiverVarsler(),
        ).tilSamletResultat()
    }
}
