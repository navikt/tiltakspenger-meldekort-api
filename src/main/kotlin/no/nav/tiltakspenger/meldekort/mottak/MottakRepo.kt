package no.nav.tiltakspenger.meldekort.mottak

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode

interface MottakRepo {
    fun lagreSak(sak: MottattSak, transactionContext: TransactionContext)

    fun oppdaterSak(sak: MottattSak, transactionContext: TransactionContext)

    fun lagreMeldeperiode(meldeperiode: Meldeperiode, transactionContext: TransactionContext)

    fun lagreMeldekortvedtak(meldekortvedtak: Meldekortvedtak, transactionContext: TransactionContext)
}
