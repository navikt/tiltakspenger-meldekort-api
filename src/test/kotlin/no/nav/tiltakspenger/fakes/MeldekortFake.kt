package no.nav.tiltakspenger.fakes

class MeldekortFake
// class MeldekortFake : BrukersMeldekortRepo {
//    private val data = Atomic(mutableMapOf<HendelseId, BrukersMeldekort>())
//
//    override fun lagreMeldekort(meldekort: Meldekort, transactionContext: TransactionContext?) {
//        data.get()[meldekort.id] = meldekort
//    }
//
//    override fun oppdaterMeldekort(meldekort: MeldekortFraUtfylling, transactionContext: TransactionContext?) {
//        val meldekortId = meldekort.id
//        val oppdaterMeldekort = data.get()[meldekortId]?.copy(
//            dager = meldekort.meldekortDager,
//        )
//
//        if (oppdaterMeldekort == null) {
//            throw NotFoundException("Fant ikke meldekort med id $meldekortId")
//        }
//
//        data.get()[meldekortId] = oppdaterMeldekort
//    }
//
//    override fun hentMeldekortForMeldeperiodeId(id: HendelseId, transactionContext: TransactionContext?): Meldekort? {
//        return data.get()[id]
//    }
//
//    override fun hentSisteMeldekort(fnr: Fnr, transactionContext: TransactionContext?): Meldekort? {
//        return data.get().values
//            .filter { meldekort -> meldekort.fnr == fnr }
//            .sortedBy { meldekort -> meldekort.periode.fraOgMed }
//            .single()
//    }
//
//    override fun hentAlleMeldekort(fnr: Fnr, transactionContext: TransactionContext?): List<Meldekort> {
//        return data.get().values
//            .filter { meldekort -> meldekort.fnr == fnr }
//    }
//
//    /**
//     * Siden 'innsendt_tidspunkt' bare lever i databasen blir ikke denn helt riktig.
//     * Da tar den bare hensyn til om statusen er 'INNSENDT', men det er kanskje greit i et test-scenario
//     */
//    override fun hentUsendteMeldekort(transactionContext: TransactionContext?): List<Meldekort> {
//        return data.get().values
//            .filter { meldekort -> meldekort.status == MeldekortStatus.INNSENDT }
//    }
//
//    override fun markerSendt(
//        id: HendelseId,
//        meldekortStatus: MeldekortStatus,
//        innsendtTidspunkt: LocalDateTime,
//        transactionContext: TransactionContext?,
//    ) {
//        val oppdaterMeldekort = data.get()[id]?.copy(
//            status = meldekortStatus,
//        )
//
//        if (oppdaterMeldekort == null) {
//            throw NotFoundException("Fant ikke meldekort med id $id")
//        }
//
//        data.get()[id] = oppdaterMeldekort
//    }
// }
