package no.nav.tiltakspenger.meldekort.microfrontend

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.separateEither
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId

class InaktiverMicrofrontendJob(
    private val microfrontendRepo: MicrofrontendRepo,
    private val tmsMikrofrontendClient: TmsMikrofrontendClient,
) {
    private val log = KotlinLogging.logger { }

    fun inaktiverMicrofrontendForBrukere(): MicrofrontendJobbResultat {
        return microfrontendRepo.hentSakerHvorMicrofrontendSkalInaktiveres().fold(
            ifLeft = { feil ->
                log.error(feil.throwable) { "Kunne ikke hente saker for inaktivering av microfrontend. Prøver igjen ved neste jobbkjøring." }
                MicrofrontendJobbResultat.tom
            },
            ifRight = { saker -> inaktiver(saker) },
        )
    }

    fun inaktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId): Either<MicrofrontendFeil, Unit> =
        tmsMikrofrontendClient.inaktiverMicrofrontendForBruker(fnr, sakId)
            .flatMap { microfrontendRepo.oppdaterStatusForMicrofrontend(sakId = sakId, aktiv = false) }

    private fun inaktiver(saker: List<MicrofrontendSak>): MicrofrontendJobbResultat {
        val (feilede, vellykkede) = saker
            .map { sak -> inaktiverMicrofrontendForBruker(sak.fnr, sak.sakId).map { sak.sakId }.mapLeft { sak.sakId to it } }
            .separateEither()

        feilede.firstOrNull()?.let { (_, førsteFeil) ->
            log.error(førsteFeil.throwable) {
                "Kunne ikke inaktivere microfrontend for ${feilede.size} av ${saker.size} sak(er): " +
                    "${feilede.map { it.first }}. Prøver igjen ved neste jobbkjøring."
            }
        }
        if (feilede.isEmpty() && vellykkede.isNotEmpty()) {
            log.info { "Inaktiverte microfrontend for ${vellykkede.size} sak(er)" }
        }

        return MicrofrontendJobbResultat(vellykkede = vellykkede, feilede = feilede.map { it.first })
    }
}
