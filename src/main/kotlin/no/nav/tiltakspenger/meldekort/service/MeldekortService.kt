package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraUtfylling
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import java.time.LocalDateTime

interface MeldekortService {

    fun lagreMeldekort(meldekort: Meldekort): Either<FeilVedLagringAvMeldekort, Unit>

    fun oppdaterMeldekort(meldekort: MeldekortFraUtfylling)

    fun hentMeldekort(id: HendelseId): Meldekort?

    fun hentSisteMeldekort(fnr: Fnr): Meldekort?

    fun hentAlleMeldekort(fnr: Fnr): List<Meldekort>

    fun hentMeldekortSomSkalSendesTilSaksbehandling(): List<Meldekort>

    fun markerSendt(id: HendelseId, meldekortStatus: MeldekortStatus, innsendtTidspunkt: LocalDateTime)
}

data object FeilVedLagringAvMeldekort
