package no.nav.tiltakspenger.meldekort.service

import ArenaMeldekortClient
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekortResponse
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.SakRepo
import no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.LandingssideStatusDTO

private const val TILTAKSPENGER_MELDEGRUPPE = "INDIV"

class FellesLandingssideService(
    val meldekortRepo: MeldekortRepo,
    val sakRepo: SakRepo,
    val arenaMeldekortClient: ArenaMeldekortClient,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentLandingssideStatus(fnr: Fnr): LandingssideStatusDTO? {
        if (!sakRepo.harSak(fnr)) {
            return null
        }

        val meldekortKlarTilInnsending = meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr)
        val harInnsendteMeldekort = meldekortRepo.hentSisteUtfylteMeldekort(fnr) != null

        val arenaMeldekort = hentArenaTiltakspengerMeldekort(fnr)
        val arenaMeldekortTilUtfylling = hentArenaMeldekortTilUtfylling(arenaMeldekort)
        val harInnsendteArenaMeldekort = harInnsendteArenaMeldekort(arenaMeldekort)

        return LandingssideStatusDTO(
            harInnsendteMeldekort = harInnsendteMeldekort || harInnsendteArenaMeldekort,
            meldekortTilUtfylling = meldekortKlarTilInnsending
                .sortedBy { it.periode.fraOgMed }
                .map { meldekort ->
                    LandingssideStatusDTO.LandingssideMeldekortDTO(
                        kanSendesFra = meldekort.meldeperiode.kanFyllesUtFraOgMed,
                    )
                } + arenaMeldekortTilUtfylling,
        )
    }

    private suspend fun hentArenaTiltakspengerMeldekort(fnr: Fnr): ArenaMeldekortResponse? {
        return arenaMeldekortClient.hentMeldekort(fnr)
            .onLeft { logger.warn { "Kunne ikke hente meldekort fra arena for landingsside - $it" } }
            .getOrNull()
            ?.takeIf { it.harTiltakspengerMeldekort() }
    }

    private fun hentArenaMeldekortTilUtfylling(response: ArenaMeldekortResponse?): List<LandingssideStatusDTO.LandingssideMeldekortDTO> {
        return response?.meldekortListe
            ?.filter { it.hoyesteMeldegruppe == TILTAKSPENGER_MELDEGRUPPE && it.mottattDato == null }
            ?.sortedBy { it.fraDato }
            ?.map { arenaMeldekort ->
                LandingssideStatusDTO.LandingssideMeldekortDTO(
                    kanSendesFra = arenaMeldekort.tilDato.plusDays(1).atStartOfDay(),
                )
            } ?: emptyList()
    }

    private fun harInnsendteArenaMeldekort(response: ArenaMeldekortResponse?): Boolean {
        return response?.meldekortListe
            ?.any { it.hoyesteMeldegruppe == TILTAKSPENGER_MELDEGRUPPE && it.mottattDato != null }
            ?: false
    }
}
