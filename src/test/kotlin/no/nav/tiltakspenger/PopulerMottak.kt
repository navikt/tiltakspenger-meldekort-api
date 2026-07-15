package no.nav.tiltakspenger

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.mottak.MottattSak
import no.nav.tiltakspenger.meldekort.sak.Sak

/**
 * Populering av testdata via mottak-skrivesiden ([no.nav.tiltakspenger.meldekort.mottak.MottakRepo]).
 *
 * Skrivemetodene `lagre`/`oppdater` på sak, meldeperiode og meldekortvedtak ble flyttet ut av lese-repoene (CQRS) og inn i mottak.
 * Disse hjelperne lar tester populere data uten å forholde seg til at writes nå går via [TestApplicationContext.mottakRepo].
 *
 * Tester bygger fortsatt opp lesemodellen [Sak] via `ObjectMother`.
 * Hjelperne projiserer den ned til skrivemodellen [MottattSak] internt, slik at kallstedene slipper å forholde seg til splittet.
 * Tester som heller vil jobbe direkte mot skrivemodellen kan bruke [MottattSak]-overloadene.
 */
fun Sak.tilMottattSak(): MottattSak = MottattSak(
    id = id,
    saksnummer = saksnummer,
    fnr = fnr,
    meldeperioder = meldeperioder,
    harSoknadUnderBehandling = harSoknadUnderBehandling,
    kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
    meldekortvedtak = meldekortvedtak,
)

fun TestApplicationContext.lagreSak(sak: Sak, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakRepo.lagreSak(sak.tilMottattSak(), tx)

        // arenaMeldekortStatus er en leseside-egenskap som settes av egen jobb (ikke noe mottak lagrer).
        // For at testdata skal kunne speile en sak som allerede har fått arena-status, setter vi den eksplisitt.
        if (sak.arenaMeldekortStatus != ArenaMeldekortStatus.UKJENT) {
            sakRepo.oppdaterArenaStatus(sak.id, sak.arenaMeldekortStatus, tx)
        }
    }
}

fun TestApplicationContext.lagreSak(mottattSak: MottattSak, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakRepo.lagreSak(mottattSak, tx)
    }
}

fun TestApplicationContext.oppdaterSak(sak: Sak, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakRepo.oppdaterSak(sak.tilMottattSak(), tx)
    }
}

fun TestApplicationContext.oppdaterSak(mottattSak: MottattSak, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakRepo.oppdaterSak(mottattSak, tx)
    }
}

fun TestApplicationContext.lagreMeldeperiode(meldeperiode: Meldeperiode, transactionContext: TransactionContext? = null) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakRepo.lagreMeldeperiode(meldeperiode, tx)
    }
}

fun TestApplicationContext.lagreMeldekortvedtak(
    meldekortvedtak: Meldekortvedtak,
    transactionContext: TransactionContext? = null,
) {
    sessionFactory.withTransactionContext(transactionContext) { tx ->
        mottakRepo.lagreMeldekortvedtak(meldekortvedtak, tx)
    }
}
