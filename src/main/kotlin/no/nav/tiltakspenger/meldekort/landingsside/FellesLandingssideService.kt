package no.nav.tiltakspenger.meldekort.landingsside

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekort
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortClient

/**
 * Aggregatet som kombinerer landingsside-saken med status fra arena.
 *
 * Selve kombineringen (Landingsside + Arena) bor her i landingsside-pakken, mens de Arena-spesifikke/rene
 * delene ligger under [no.nav.tiltakspenger.meldekort.arena] slik at de forsvinner samlet når arena-pakken slettes.
 */
class FellesLandingssideService(
    private val landingssideRepo: LandingssideRepo,
    private val arenaMeldekortClient: ArenaMeldekortClient,
    private val redirectUrl: String,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentLandingssideStatus(fnr: Fnr): LandingssideStatus? {
        val sak = landingssideRepo.hentSak(fnr)
            ?: return hentStatusFraArena(fnr)?.tilLandingssideStatus(redirectUrl)

        // Vi trenger ikke spørre arena på nytt dersom saken allerede har sjekket at brukeren ikke har meldekort der
        val statusFraArena = if (sak.harIkkeMeldekortIArena) {
            null
        } else {
            hentStatusFraArena(fnr)
        }

        return sak.tilLandingssideStatus(
            arenaStatus = statusFraArena,
            redirectUrl = redirectUrl,
        )
    }

    private suspend fun hentStatusFraArena(fnr: Fnr): LandingssideArenaStatus? {
        val arenaMeldekort = arenaMeldekortClient.hentMeldekort(fnr).getOrElse {
            logger.warn { "Henting av meldekort fra arena til landingsside feilet - $it" }
            null
        } ?: return null

        val tiltakspengerMeldekort = arenaMeldekort.hentTiltakspengerMeldekort()

        return LandingssideArenaStatus.create(
            harMeldekortIArena = tiltakspengerMeldekort != null,
            harInnsendteMeldekort = arenaMeldekort.harInnsendteTiltakspengerMeldekort() ||
                harInnsendteHistoriskeArenaMeldekort(fnr),
            meldekortTilUtfylling = tiltakspengerMeldekort.tilLandingssideMeldekort(),
        )
    }

    private suspend fun harInnsendteHistoriskeArenaMeldekort(fnr: Fnr): Boolean {
        return arenaMeldekortClient.hentHistoriskeMeldekort(fnr).map {
            it?.harInnsendteTiltakspengerMeldekort() == true
        }.getOrElse {
            logger.warn { "Kunne ikke hente historiske meldekort fra arena - $it" }
            false
        }
    }

    private fun List<ArenaMeldekort>?.tilLandingssideMeldekort(): List<LandingssideMeldekort> {
        return this.orEmpty().mapNotNull { meldekort ->
            if (meldekort.mottattDato != null) {
                return@mapNotNull null
            }

            LandingssideMeldekort(
                kanSendesFra = meldekort.kanSendesFra(),
            )
        }
    }
}
