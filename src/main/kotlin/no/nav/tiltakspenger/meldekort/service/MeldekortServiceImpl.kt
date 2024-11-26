package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo

class MeldekortServiceImpl(
    val meldekortRepo: MeldekortRepo,
) : MeldekortService {
    override fun lagreMeldekort(meldekort: Meldekort) {
        meldekortRepo.lagreMeldekort(meldekort = meldekort)
    }

    override fun hentMeldekort(id: String): Meldekort? {
        return meldekortRepo.hentMeldekort(id)
    }
}
