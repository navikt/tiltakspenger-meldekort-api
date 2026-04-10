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

    fun kjørSendVarsler(tac: TestApplicationContext) {
        tac.sendVarslerService.sendVarselForMeldekort()
    }

    fun kjørInaktiverVarsler(tac: TestApplicationContext) {
        tac.inaktiverVarslerService.inaktiverVarslerForMottatteMeldekort()
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
