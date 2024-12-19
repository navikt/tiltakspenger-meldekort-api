package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import io.ktor.server.plugins.NotFoundException
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortFraUtfyllingDTO
import java.time.LocalDateTime

class MeldekortFake : MeldekortRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, Meldekort>())

    override fun lagreMeldekort(meldekort: Meldekort, transactionContext: TransactionContext?) {
        data.get()[meldekort.id] = meldekort
    }

    override fun oppdaterMeldekort(meldekort: MeldekortFraUtfyllingDTO, transactionContext: TransactionContext?) {
        // TODO KEW: Fiks opp i denne når den tar inn domene/base versjon av meldekortFraUtfylling.
//        val meldekortId = MeldekortId.fromString(meldekort.id)
//        val oppdaterMeldekort = data.get()[meldekortId]?.copy(
//            meldekortDager = meldekort.meldekortDager
//        )
//
//        if (oppdaterMeldekort == null) {
//            throw NotFoundException("Fant ikke meldekort med id $meldekortId")
//        }
//
//        data.get()[meldekortId] = oppdaterMeldekort
    }

    override fun hentMeldekort(meldekortId: MeldekortId, transactionContext: TransactionContext?): Meldekort? {
        return data.get()[meldekortId]
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
        meldekortId: MeldekortId,
        meldekortStatus: MeldekortStatus,
        innsendtTidspunkt: LocalDateTime,
        transactionContext: TransactionContext?,
    ) {
        val oppdaterMeldekort = data.get()[meldekortId]?.copy(
            status = meldekortStatus,
        )

        if (oppdaterMeldekort == null) {
            throw NotFoundException("Fant ikke meldekort med id $meldekortId")
        }

        data.get()[meldekortId] = oppdaterMeldekort
    }
}
