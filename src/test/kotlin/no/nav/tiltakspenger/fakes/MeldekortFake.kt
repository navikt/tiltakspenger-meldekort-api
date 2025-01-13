package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import io.ktor.server.plugins.NotFoundException
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraUtfylling
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.LocalDateTime

class MeldekortFake : MeldekortRepo {
    private val data = Atomic(mutableMapOf<HendelseId, Meldekort>())

    override fun lagreMeldekort(meldekort: Meldekort, transactionContext: TransactionContext?) {
        data.get()[meldekort.id] = meldekort
    }

    override fun oppdaterMeldekort(meldekort: MeldekortFraUtfylling, transactionContext: TransactionContext?) {
        val meldekortId = meldekort.id
        val oppdaterMeldekort = data.get()[meldekortId]?.copy(
            meldekortDager = meldekort.meldekortDager,
        )

        if (oppdaterMeldekort == null) {
            throw NotFoundException("Fant ikke meldekort med id $meldekortId")
        }

        data.get()[meldekortId] = oppdaterMeldekort
    }

    override fun hentMeldekort(id: HendelseId, transactionContext: TransactionContext?): Meldekort? {
        return data.get()[id]
    }

    override fun hentSisteMeldekort(fnr: Fnr, transactionContext: TransactionContext?): Meldekort? {
        return data.get().values
            .filter { meldekort -> meldekort.fnr == fnr }
            .sortedBy { meldekort -> meldekort.fraOgMed }
            .single()
    }

    override fun hentAlleMeldekort(fnr: Fnr, transactionContext: TransactionContext?): List<Meldekort> {
        return data.get().values
            .filter { meldekort -> meldekort.fnr == fnr }
    }

    /**
     * Siden 'innsendt_tidspunkt' bare lever i databasen blir ikke denn helt riktig.
     * Da tar den bare hensyn til om statusen er 'INNSENDT', men det er kanskje greit i et test-scenario
     */
    override fun hentUsendteMeldekort(transactionContext: TransactionContext?): List<Meldekort> {
        return data.get().values
            .filter { meldekort -> meldekort.status == MeldekortStatus.INNSENDT }
    }

    override fun markerSendt(
        id: HendelseId,
        meldekortStatus: MeldekortStatus,
        innsendtTidspunkt: LocalDateTime,
        transactionContext: TransactionContext?,
    ) {
        val oppdaterMeldekort = data.get()[id]?.copy(
            status = meldekortStatus,
        )

        if (oppdaterMeldekort == null) {
            throw NotFoundException("Fant ikke meldekort med id $id")
        }

        data.get()[id] = oppdaterMeldekort
    }
}
