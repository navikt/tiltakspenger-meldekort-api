package no.nav.tiltakspenger.meldekort.api.jobber

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.libs.jobber.StoppableJob
import no.nav.tiltakspenger.libs.jobber.startStoppableJob
import no.nav.tiltakspenger.meldekort.api.service.MeldekortService
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

internal class GenererMeldekortJobb(
    private val stoppableJob: StoppableJob,
) : StoppableJob by stoppableJob {
    companion object {
        fun startJob(
            runCheckFactory: RunCheckFactory,
            meldekortService: MeldekortService,
            initialDelay: Duration = 1.minutes,
            intervall: Duration = 1.hours,
        ): GenererMeldekortJobb {
            val logger = KotlinLogging.logger { }
            return GenererMeldekortJobb(
                startStoppableJob(
                    jobName = "generer-meldekort",
                    initialDelay = initialDelay.toJavaDuration(),
                    intervall = intervall.toJavaDuration(),
                    logger = logger,
                    sikkerLogg = KotlinLogging.logger("tjenestekall"),
                    mdcCallIdKey = "call-id",
                    runJobCheck = listOf(runCheckFactory.leaderPod()),
                    job = {
                        val iDag = LocalDate.now()
                        if (iDag.dayOfWeek == DayOfWeek.MONDAY) {
                            meldekortService.genererMeldekort(iDag)
                        } else {
                            logger.debug { "GenererMeldekortJobb: Venter til mandag med å generere meldekort. iDag: $iDag" }
                        }
                    },
                ),
            )
        }
    }
}
