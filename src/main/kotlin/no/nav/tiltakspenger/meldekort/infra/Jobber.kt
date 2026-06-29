package no.nav.tiltakspenger.meldekort.infra

import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task

fun jobber(applicationContext: ApplicationContext): List<Task> {
    return listOf(
        Task(
            navn = "send-meldekort-jobb",
            utfør = {
                applicationContext.sendMeldekortService.sendMeldekort()
                TaskResultat.Ferdig
            },
        ),
        Task(
            navn = "journalfør-meldekort-jobb",
            utfør = {
                applicationContext.journalførMeldekortService.journalførNyeMeldekort()
                TaskResultat.Ferdig
            },
        ),
        Task(
            navn = "varsel-jobber",
            utfør = {
                applicationContext.varselJobber.kjørAlle()
                TaskResultat.Ferdig
            },
        ),
        Task(
            navn = "oppdater-arena-meldekort-status-jobb",
            utfør = {
                applicationContext.arenaMeldekortStatusService.oppdaterArenaMeldekortStatusForSaker()
                TaskResultat.Ferdig
            },
        ),
        Task(
            navn = "aktiver-microfrontend-jobb",
            utfør = {
                applicationContext.aktiverMicrofrontendJob.aktiverMicrofrontendForBrukere()
                TaskResultat.Ferdig
            },
        ),
        Task(
            navn = "inaktiver-microfrontend-jobb",
            utfør = {
                applicationContext.inaktiverMicrofrontendJob.inaktiverMicrofrontendForBrukere()
                TaskResultat.Ferdig
            },
        ),
    )
}
