package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortFraUtfyllingDTO
import java.time.LocalDateTime

interface MeldekortService {

    fun lagreMeldekort(meldekort: Meldekort): Either<FeilVedLagringAvMeldekort, Unit>

    fun oppdaterMeldekort(meldekort: MeldekortFraUtfyllingDTO)

    fun hentMeldekort(meldekortId: MeldekortId): Meldekort?

    fun hentSisteMeldekort(fnr: Fnr): Meldekort?

    fun hentAlleMeldekort(fnr: Fnr): List<Meldekort>

    fun hentMeldekortSomSkalSendesTilSaksbehandling(): List<Meldekort>

    fun markerSendt(meldekortId: MeldekortId, meldekortStatus: MeldekortStatus, innsendtTidspunkt: LocalDateTime)
}

data object FeilVedLagringAvMeldekort;