package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraUtfylling
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.LocalDateTime

class MeldekortServiceImpl(
    val meldekortRepo: MeldekortRepo,
) : MeldekortService {
    private val logger = KotlinLogging.logger { }

    override fun lagreMeldekort(meldekort: Meldekort): Either<FeilVedLagringAvMeldekort, Unit> {
        return Either.catch {
            meldekortRepo.lagreMeldekort(meldekort)
            logger.info { "Lagret meldekort: ${meldekort.id}" }
        }.mapLeft {
            with("Lagring av meldekort feilet: ${meldekort.id}") {
                logger.error { this }
                sikkerlogg.error(it) { this }
            }
            FeilVedLagringAvMeldekort
        }
    }

    override fun oppdaterMeldekort(meldekort: MeldekortFraUtfylling) {
        meldekortRepo.oppdaterMeldekort(meldekort)
    }

    override fun hentMeldekort(id: HendelseId): Meldekort? {
        return meldekortRepo.hentMeldekort(id)
    }

    override fun hentSisteMeldekort(fnr: Fnr): Meldekort? {
        return meldekortRepo.hentSisteMeldekort(fnr)
    }

    override fun hentAlleMeldekort(fnr: Fnr): List<Meldekort> {
        return meldekortRepo.hentAlleMeldekort(fnr)
    }

    override fun hentMeldekortSomSkalSendesTilSaksbehandling(): List<Meldekort> {
        return meldekortRepo.hentUsendteMeldekort()
    }

    override fun markerSendt(
        id: HendelseId,
        meldekortStatus: MeldekortStatus,
        innsendtTidspunkt: LocalDateTime,
    ) {
        meldekortRepo.markerSendt(id, meldekortStatus, innsendtTidspunkt)
    }
}
