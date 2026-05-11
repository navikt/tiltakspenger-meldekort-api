package no.nav.tiltakspenger.meldekort.landingsside

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekort
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortRepo
import no.nav.tiltakspenger.meldekort.sak.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.sak.SakRepo

class FellesLandingssideService(
    private val meldekortRepo: MeldekortRepo,
    private val sakRepo: SakRepo,
    private val arenaMeldekortClient: ArenaMeldekortClient,
    private val redirectUrl: String,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentLandingssideStatus(fnr: Fnr): LandingssideStatus? {
        val sak = sakRepo.hentForBruker(fnr)

        // Vi trenger ikke spørre arena på nytt dersom saken allerede har sjekket at brukeren ikke har meldekort der
        val statusFraArena = if (sak?.arenaMeldekortStatus == ArenaMeldekortStatus.HAR_IKKE_MELDEKORT) {
            null
        } else {
            hentFraArena(fnr)
        }

        if (sak == null) {
            return statusFraArena
        }

        val harInnsendteMeldekort = meldekortRepo.hentSisteUtfylteMeldekort(fnr) != null

        val meldekortTilUtfylling = meldekortRepo
            .hentAlleMeldekortKlarTilInnsending(fnr)
            .map {
                LandingssideMeldekort(kanSendesFra = it.meldeperiode.kanFyllesUtFraOgMed)
            }

        return LandingssideStatus(
            harInnsendteMeldekort = harInnsendteMeldekort || statusFraArena?.harInnsendteMeldekort == true,
            meldekortTilUtfylling = meldekortTilUtfylling.plus(
                statusFraArena?.meldekortTilUtfylling.orEmpty(),
            ).sortedBy { it.kanSendesFra },
            redirectUrl = redirectUrl,
        )
    }

    private suspend fun hentFraArena(fnr: Fnr): LandingssideStatus? {
        val arenaMeldekortResponse = arenaMeldekortClient.hentMeldekort(fnr).getOrElse {
            logger.warn { "Henting av meldekort fra arena til landingsside feilet - $it" }
            null
        }

        if (arenaMeldekortResponse == null) {
            return null
        }

        val meldekortFraArena = arenaMeldekortResponse.hentTiltakspengerMeldekort()

        val harInnsendteMeldekort =
            meldekortFraArena?.harInnsendteMeldekort() == true || harInnsendteHistoriskeArenaMeldekort(fnr)

        if (!harInnsendteMeldekort && meldekortFraArena == null) {
            return null
        }

        return LandingssideStatus(
            harInnsendteMeldekort = harInnsendteMeldekort,
            meldekortTilUtfylling = meldekortFraArena?.mapNotNull {
                if (it.mottattDato != null) {
                    return@mapNotNull null
                }

                LandingssideMeldekort(
                    kanSendesFra = it.kanSendesFra(),
                )
            }?.sortedBy { it.kanSendesFra } ?: emptyList(),
            redirectUrl = redirectUrl,
        )
    }

    private suspend fun harInnsendteHistoriskeArenaMeldekort(fnr: Fnr): Boolean {
        return arenaMeldekortClient.hentHistoriskeMeldekort(fnr).map {
            it?.meldekortListe?.harInnsendteMeldekort() == true
        }.getOrElse {
            logger.warn { "Kunne ikke hente historiske meldekort fra arena - $it" }
            false
        }
    }

    private fun List<ArenaMeldekort>.harInnsendteMeldekort(): Boolean {
        return this.any { it.erTiltakspengerMeldekort() && it.mottattDato != null }
    }
}
