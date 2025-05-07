package no.nav.tiltakspenger.meldekort.service

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus

class ArenaMeldekortStatusService(
    private val arenaMeldekortClient: ArenaMeldekortClient,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentArenaMeldekortStatus(fnr: Fnr): ArenaMeldekortStatus {
        arenaMeldekortClient.hentMeldekort(fnr).onLeft {
            logger.error { "Kunne ikke hente meldekort fra arena - $it" }
            return ArenaMeldekortStatus.UKJENT
        }.onRight {
            if (it == null) {
                return ArenaMeldekortStatus.HAR_IKKE_MELDEKORT
            } else if (it.harTiltakspengerMeldekort()) {
                return ArenaMeldekortStatus.HAR_MELDEKORT
            }
        }

        val historiskeMeldekort = arenaMeldekortClient.hentHistoriskeMeldekort(fnr).getOrElse {
            logger.error { "Kunne ikke hente historiske meldekort fra arena - $it" }
            return ArenaMeldekortStatus.UKJENT
        }

        return if (historiskeMeldekort?.harTiltakspengerMeldekort() != true) {
            ArenaMeldekortStatus.HAR_IKKE_MELDEKORT
        } else {
            ArenaMeldekortStatus.HAR_MELDEKORT
        }
    }
}
