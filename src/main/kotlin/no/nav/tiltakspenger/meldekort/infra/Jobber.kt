package no.nav.tiltakspenger.meldekort.infra

import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.meldekort.jobb.JobbResultat
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendJobbResultat

fun jobber(applicationContext: ApplicationContext): List<Task> {
    return listOf(
        Task(
            navn = "send-meldekort-jobb",
            utfør = {
                applicationContext.sendMeldekortJobb.sendMeldekort().tilTaskResultat()
            },
        ),
        Task(
            navn = "journalfør-meldekort-jobb",
            utfør = {
                applicationContext.journalførMeldekortService.journalførNyeMeldekort().tilTaskResultat()
            },
        ),
        Task(
            navn = "varsel-jobber",
            utfør = {
                applicationContext.varselJobber.kjørAlle().tilTaskResultat()
            },
        ),
        Task(
            navn = "oppdater-arena-meldekort-status-jobb",
            utfør = {
                applicationContext.arenaMeldekortStatusService.oppdaterArenaMeldekortStatusForSaker().tilTaskResultat()
            },
        ),
        Task(
            navn = "aktiver-microfrontend-jobb",
            utfør = {
                applicationContext.aktiverMicrofrontendJob.aktiverMicrofrontendForBrukere().tilTaskResultat()
            },
        ),
        Task(
            navn = "inaktiver-microfrontend-jobb",
            utfør = {
                applicationContext.inaktiverMicrofrontendJob.inaktiverMicrofrontendForBrukere().tilTaskResultat()
            },
        ),
    )
}

/**
 * Mapper domenets [JobbResultat] til scheduler-bibliotekets [TaskResultat].
 */
private fun JobbResultat.tilTaskResultat(): TaskResultat = when (this) {
    JobbResultat.IngenArbeid -> TaskResultat.IngenArbeid
    JobbResultat.UtførteArbeid -> TaskResultat.Ferdig
    JobbResultat.Feilet -> TaskResultat.Feilet
}

/**
 * En microfrontend-jobb feilet dersom den ikke fikk hentet saker, og hadde arbeid dersom den forsøkte å håndtere minst én sak (uavhengig av om forsøket lyktes).
 */
private fun MicrofrontendJobbResultat.tilTaskResultat(): TaskResultat = when {
    kunneIkkeHenteSaker -> TaskResultat.Feilet
    vellykkede.isEmpty() && feilede.isEmpty() -> TaskResultat.IngenArbeid
    else -> TaskResultat.Ferdig
}
