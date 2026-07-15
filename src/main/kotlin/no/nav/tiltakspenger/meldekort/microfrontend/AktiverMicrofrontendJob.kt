package no.nav.tiltakspenger.meldekort.microfrontend

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.separateEither
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId

class AktiverMicrofrontendJob(
    private val microfrontendRepo: MicrofrontendRepo,
    private val tmsMikrofrontendClient: TmsMikrofrontendClient,
) {
    private val log = KotlinLogging.logger { }

    fun aktiverMicrofrontendForBrukere(): MicrofrontendJobbResultat {
        return microfrontendRepo.hentSakerHvorMicrofrontendSkalAktiveres().fold(
            ifLeft = { feil ->
                log.error(feil.throwable) { "Kunne ikke hente saker for aktivering av microfrontend. Prøver igjen ved neste jobbkjøring." }
                MicrofrontendJobbResultat.henteFeil
            },
            ifRight = { saker -> aktiver(saker) },
        )
    }

    fun aktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId): Either<MicrofrontendFeil, Unit> =
        tmsMikrofrontendClient.aktiverMicrofrontendForBruker(fnr, sakId)
            .flatMap { microfrontendRepo.oppdaterStatusForMicrofrontend(sakId = sakId, aktiv = true) }

    private fun aktiver(saker: List<MicrofrontendSak>): MicrofrontendJobbResultat {
        val (feilede, vellykkede) = saker
            .map { sak -> aktiverMicrofrontendForBruker(sak.fnr, sak.sakId).map { sak.sakId }.mapLeft { sak.sakId to it } }
            .separateEither()

        feilede.firstOrNull()?.let { (_, førsteFeil) ->
            log.error(førsteFeil.throwable) {
                "Kunne ikke aktivere microfrontend for ${feilede.size} av ${saker.size} sak(er): " +
                    "${feilede.map { it.first }}. Prøver igjen ved neste jobbkjøring."
            }
        }
        if (feilede.isEmpty() && vellykkede.isNotEmpty()) {
            log.info { "Aktiverte microfrontend for ${vellykkede.size} sak(er)" }
        }

        return MicrofrontendJobbResultat(vellykkede = vellykkede, feilede = feilede.map { it.first }, kunneIkkeHenteSaker = false)
    }
}
