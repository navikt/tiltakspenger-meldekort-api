package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortFraUtfyllingDTO

class MeldekortServiceImpl(
    val meldekortRepo: MeldekortRepo,
) : MeldekortService {
    override fun lagreMeldekort(meldekort: Meldekort) {
        meldekortRepo.lagreMeldekort(meldekort)
    }

    override fun oppdaterMeldekort(meldekort: MeldekortFraUtfyllingDTO) {
        meldekortRepo.oppdaterMeldekort(meldekort)
    }

    override fun hentMeldekort(meldekortId: MeldekortId): Meldekort? {
        return meldekortRepo.hentMeldekort(meldekortId)
    }

    override fun hentSisteMeldekort(fnr: Fnr): Meldekort? {
        return meldekortRepo.hentSisteMeldekort(fnr)
    }

    override fun hentAlleMeldekort(fnr: Fnr): List<Meldekort> {
        return meldekortRepo.hentAlleMeldekort(fnr)
    }
}
