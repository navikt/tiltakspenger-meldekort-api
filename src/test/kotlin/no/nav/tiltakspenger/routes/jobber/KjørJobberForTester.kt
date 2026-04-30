package no.nav.tiltakspenger.routes.jobber

import no.nav.tiltakspenger.TestApplicationContext

/**
 * Emulerer Application.kt sin TaskExecutor.startJob(...)
 */
object KjørJobberForTester {

    suspend fun kjørSendMeldekortTilSaksbehandling(tac: TestApplicationContext) {
        tac.sendMeldekortService.sendMeldekort()
    }

    suspend fun kjørJournalførMeldekort(tac: TestApplicationContext) {
        tac.journalførMeldekortService.journalførNyeMeldekort()
    }

    fun kjørAktiverVarsler(tac: TestApplicationContext) {
        tac.aktiverVarslerService.aktiverVarsler()
    }

    fun kjørInaktiverVarsler(tac: TestApplicationContext) {
        tac.inaktiverVarslerService.inaktiverVarsler()
    }

    /**
     * Kjører alle varseljobbene i samme rekkefølge som i prod (VurderVarselService -> AktiverVarslerService -> InaktiverVarslerService).
     */
    fun kjørVarsler(tac: TestApplicationContext) {
        tac.varselJobber.kjørAlle()
    }

    fun kjørVurderVarsler(tac: TestApplicationContext) {
        tac.vurderVarselService.vurderVarsler()
    }

    suspend fun kjørOppdaterArenaMeldekortStatus(tac: TestApplicationContext) {
        tac.arenaMeldekortStatusService.oppdaterArenaMeldekortStatusForSaker()
    }

    fun kjørAktiverMicrofrontend(tac: TestApplicationContext) {
        tac.aktiverMicrofrontendJob.aktiverMicrofrontendForBrukere()
    }

    fun kjørInaktiverMicrofrontend(tac: TestApplicationContext) {
        tac.inaktiverMicrofrontendJob.inaktiverMicrofrontendForBrukere()
    }
}
