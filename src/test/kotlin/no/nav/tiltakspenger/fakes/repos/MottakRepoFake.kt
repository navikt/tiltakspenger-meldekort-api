package no.nav.tiltakspenger.fakes.repos

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.mottak.MottakRepo
import no.nav.tiltakspenger.meldekort.mottak.MottattSak

/**
 * In-memory speiling av [no.nav.tiltakspenger.meldekort.mottak.infra.MottakPostgresRepo].
 *
 * Skrivesiden (mottak fra saksbehandling-api) deler underliggende state med lese-fakene ved å delegere til de samme [SakRepoFake]/[MeldeperiodeRepoFake]/[MeldekortvedtakRepoFake]-instansene som koden under test leser fra.
 */
class MottakRepoFake(
    private val sakRepo: SakRepoFake,
    private val meldeperiodeRepo: MeldeperiodeRepoFake,
    private val meldekortvedtakRepo: MeldekortvedtakRepoFake,
) : MottakRepo {
    override fun lagreSak(sak: MottattSak, transactionContext: TransactionContext) {
        sakRepo.lagre(sak)
    }

    override fun oppdaterSak(sak: MottattSak, transactionContext: TransactionContext) {
        sakRepo.oppdater(sak)
    }

    override fun lagreMeldeperiode(meldeperiode: Meldeperiode, transactionContext: TransactionContext) {
        meldeperiodeRepo.lagre(meldeperiode)
    }

    override fun lagreMeldekortvedtak(meldekortvedtak: Meldekortvedtak, transactionContext: TransactionContext) {
        meldekortvedtakRepo.lagre(meldekortvedtak, transactionContext)
    }
}
