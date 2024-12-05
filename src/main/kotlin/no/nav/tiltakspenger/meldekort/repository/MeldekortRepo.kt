package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortFraUtfyllingDTO
import java.time.LocalDateTime

interface MeldekortRepo {
    fun lagreMeldekort(
        meldekort: Meldekort,
        transactionContext: TransactionContext? = null,
    )

    fun oppdaterMeldekort(
        meldekort: MeldekortFraUtfyllingDTO,
        transactionContext: TransactionContext? = null,
    )

    fun hentMeldekort(
        meldekortId: MeldekortId,
        transactionContext: TransactionContext? = null,
    ): Meldekort?

    fun hentSisteMeldekort(
        fnr: Fnr,
        transactionContext: TransactionContext? = null,
    ): Meldekort?

    fun hentAlleMeldekort(
        fnr: Fnr,
        transactionContext: TransactionContext? = null,
    ): List<Meldekort>

    fun hentUsendteMeldekort(transactionContext: TransactionContext? = null): List<Meldekort>

    fun markerSendt(meldekortId: MeldekortId, meldekortStatus: MeldekortStatus, innsendtTidspunkt: LocalDateTime, transactionContext: TransactionContext? = null)
}
