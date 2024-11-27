package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.Meldekort

interface MeldekortRepo {
    fun lagreMeldekort(
        meldekort: Meldekort,
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
}
