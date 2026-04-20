package no.nav.tiltakspenger.meldekort.service

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekort
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.SakRepo
import no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.LandingssideStatusDTO
import no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.LandingssideStatusDTO.LandingssideMeldekortDTO

class FellesLandingssideService(
    val meldekortRepo: MeldekortRepo,
    val sakRepo: SakRepo,
    val arenaMeldekortClient: ArenaMeldekortClient,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentLandingssideStatus(fnr: Fnr): LandingssideStatusDTO? {
        val sak = sakRepo.hentForBruker(fnr)

        // Vi trenger ikke spørre arena på nytt dersom saken allerede har sjekket at brukeren ikke har meldekort der
        val statusFraArena = if (sak?.arenaMeldekortStatus != ArenaMeldekortStatus.HAR_IKKE_MELDEKORT) {
            hentFraArena(fnr)
        } else {
            null
        }

        if (sak == null && statusFraArena == null) {
            return null
        }

        val harInnsendteMeldekort = meldekortRepo.hentSisteUtfylteMeldekort(fnr) != null

        val meldekortTilUtfylling = meldekortRepo
            .hentAlleMeldekortKlarTilInnsending(fnr)
            .map {
                LandingssideMeldekortDTO(
                    kanSendesFra = it.meldeperiode.kanFyllesUtFraOgMed,
                )
            }

        return LandingssideStatusDTO(
            harInnsendteMeldekort = harInnsendteMeldekort || statusFraArena?.harInnsendteMeldekort == true,
            meldekortTilUtfylling = meldekortTilUtfylling.plus(
                statusFraArena?.meldekortTilUtfylling.orEmpty(),
            ).sortedBy { it.kanSendesFra },
        )
    }

    private suspend fun hentFraArena(fnr: Fnr): LandingssideStatusDTO? {
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

        return LandingssideStatusDTO(
            harInnsendteMeldekort = harInnsendteMeldekort,
            meldekortTilUtfylling = meldekortFraArena?.mapNotNull {
                if (it.mottattDato != null) {
                    return@mapNotNull null
                }

                LandingssideMeldekortDTO(
                    kanSendesFra = it.kanSendesFra(),
                )
            } ?: emptyList(),
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
