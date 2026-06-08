package no.nav.tiltakspenger.meldekort.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId

class HentMeldekortService(
    private val meldekortRepo: MeldekortRepo,
) {
    fun hentForMeldekortId(id: MeldekortId, fnr: Fnr): BrukersMeldekort? {
        return meldekortRepo.hentForMeldekortId(id, fnr)
    }

    fun hentSisteUtfylteMeldekort(fnr: Fnr): BrukersMeldekort? {
        return meldekortRepo.hentSisteUtfylteMeldekort(fnr)
    }

    fun hentNesteMeldekortForUtfylling(fnr: Fnr): BrukersMeldekort? {
        return meldekortRepo.hentNesteMeldekortTilUtfylling(fnr)
    }

    fun hentInnsendteMeldekort(fnr: Fnr): List<MeldekortMedSisteMeldeperiode> {
        return meldekortRepo.hentInnsendteMeldekortForBruker(fnr)
    }

    fun hentAlleMeldekortKlarTilInnsending(fnr: Fnr): List<BrukersMeldekort> {
        return meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr)
    }

    fun hentMeldekortForKjede(kjedeId: MeldeperiodeKjedeId, fnr: Fnr): MeldekortForKjede {
        return meldekortRepo.hentMeldekortForKjedeId(kjedeId, fnr)
    }
}
