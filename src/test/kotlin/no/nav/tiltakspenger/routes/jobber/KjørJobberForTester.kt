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
        tac.aktiverVarslerService.sendVarselForMeldekort()
    }

    fun kjørInaktiverVarsler(tac: TestApplicationContext) {
        tac.inaktiverVarslerService.inaktiverVarsler()
    }

    /**
     * Kjører både send varsler og inaktiverer varsler i den rekkefølgen (slik som i prod)
     */
    fun kjørVarsler(tac: TestApplicationContext) {
        kjørVurderVarsler(tac)
        kjørAktiverVarsler(tac)
        kjørInaktiverVarsler(tac)
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
