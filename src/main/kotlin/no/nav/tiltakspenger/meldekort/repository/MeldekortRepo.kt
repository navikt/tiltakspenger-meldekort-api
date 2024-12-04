package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortFraUtfyllingDTO
import java.time.LocalDateTime

interface MeldekortRepo {
    fun lagreMeldekort(
        meldeperiode: Meldeperiode,
        transactionContext: TransactionContext? = null,
    )

    fun oppdaterMeldekort(
        meldekort: MeldekortFraUtfyllingDTO,
        transactionContext: TransactionContext? = null,
    )

    fun hentMeldekort(
        meldekortId: MeldekortId,
        transactionContext: TransactionContext? = null,
    ): Meldeperiode?

    fun hentSisteMeldekort(
        fnr: Fnr,
        transactionContext: TransactionContext? = null,
    ): Meldeperiode?

    fun hentAlleMeldekort(
        fnr: Fnr,
        transactionContext: TransactionContext? = null,
    ): List<Meldeperiode>

    fun hentUsendteMeldekort(transactionContext: TransactionContext? = null): List<Meldeperiode>

    fun markerSendt(meldekortId: MeldekortId, meldekortStatus: MeldekortStatus, tidspunkt: LocalDateTime, transactionContext: TransactionContext? = null)
}
