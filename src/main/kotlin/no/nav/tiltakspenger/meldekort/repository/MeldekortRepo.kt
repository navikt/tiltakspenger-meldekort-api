package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.Meldekort

interface MeldekortRepo {
    fun lagreMeldekort(
        meldekort: Meldekort,
        transactionContext: TransactionContext? = null,
    )

    fun hentMeldekort(
        meldekortId: String,
        transactionContext: TransactionContext? = null,
    ): Meldekort?

    fun hentSisteMeldekort(
        fnr: String,
        transactionContext: TransactionContext? = null,
    ): Meldekort?

    fun hentAlleMeldekort(
        fnr: String,
        transactionContext: TransactionContext? = null,
    ): List<Meldekort>
}
