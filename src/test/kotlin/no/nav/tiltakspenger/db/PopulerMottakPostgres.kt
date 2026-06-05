package no.nav.tiltakspenger

import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.mottak.MottattSak
import no.nav.tiltakspenger.meldekort.sak.Sak

/**
 * Populering av testdata mot Postgres via mottak-skrivesiden
 * ([no.nav.tiltakspenger.meldekort.mottak.infra.MottakPostgresRepo]).
 *
 * Skrivemetodene `lagre`/`oppdater` på sak, meldeperiode og meldekortvedtak ble flyttet ut av
 * lese-repoene (CQRS) og inn i mottak. Disse hjelperne lar tester populere data uten å forholde seg
 * til at writes nå går via [TestDataHelper.mottakPostgresRepo].
 *
 * Tester som heller vil jobbe direkte mot skrivemodellen kan bruke [MottattSak]-overloadene.
 */
fun TestDataHelper.lagreSak(sak: Sak, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakPostgresRepo.lagreSak(sak.tilMottattSak(), tx)
        // arenaMeldekortStatus er en leseside-egenskap som settes av egen jobb (ikke noe mottak lagrer).
        // For at testdata skal kunne speile en sak som allerede har fått arena-status, setter vi den eksplisitt.
        if (sak.arenaMeldekortStatus != ArenaMeldekortStatus.UKJENT) {
            sakPostgresRepo.oppdaterArenaStatus(sak.id, sak.arenaMeldekortStatus, tx)
        }
    }
}

fun TestDataHelper.lagreSak(mottattSak: MottattSak, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakPostgresRepo.lagreSak(mottattSak, tx)
    }
}

fun TestDataHelper.oppdaterSak(sak: Sak, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakPostgresRepo.oppdaterSak(sak.tilMottattSak(), tx)
    }
}

fun TestDataHelper.oppdaterSak(mottattSak: MottattSak, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakPostgresRepo.oppdaterSak(mottattSak, tx)
    }
}

fun TestDataHelper.lagreMeldeperiode(meldeperiode: Meldeperiode, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakPostgresRepo.lagreMeldeperiode(meldeperiode, tx)
    }
}

fun TestDataHelper.lagreMeldekortvedtak(meldekortvedtak: Meldekortvedtak, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakPostgresRepo.lagreMeldekortvedtak(meldekortvedtak, tx)
    }
}
