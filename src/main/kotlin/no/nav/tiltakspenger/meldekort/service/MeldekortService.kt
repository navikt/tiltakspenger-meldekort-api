package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.validerLagring
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.LocalDateTime

class MeldekortService(
    val meldekortRepo: MeldekortRepo,
) {
    fun lagreMeldekortFraBruker(kommando: LagreMeldekortFraBrukerKommando) {
        val meldekortId = kommando.id
        val innsenderFnr = kommando.fnr
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, innsenderFnr)

        requireNotNull(meldekort) {
            "Meldekort med id $meldekortId finnes ikke for bruker ${innsenderFnr.verdi}"
        }

        require(meldekort.mottatt == null) {
            "Meldekort med id $meldekortId er allerede mottatt"
        }

        meldekort.validerLagring(kommando).also {
            meldekortRepo.lagreFraBruker(kommando)
        }
    }

    fun hentForMeldekortId(id: MeldekortId, fnr: Fnr): Meldekort? {
        return meldekortRepo.hentForMeldekortId(id, fnr)
    }

    fun hentSisteMeldekort(fnr: Fnr): Meldekort? {
        return meldekortRepo.hentSisteMeldekort(fnr)
    }

    fun hentNesteMeldekortForUtfylling(fnr: Fnr): Meldekort? {
        return meldekortRepo.hentNesteMeldekortTilUtfylling(fnr)
    }

    fun hentAlleMeldekort(fnr: Fnr): List<Meldekort> {
        return meldekortRepo.hentAlleMeldekortForBruker(fnr)
    }

    fun hentMeldekortSomSkalSendesTilSaksbehandling(): List<Meldekort> {
        return meldekortRepo.hentMeldekortForSendingTilSaksbehandling()
    }

    fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
    ) {
        meldekortRepo.markerSendtTilSaksbehandling(id, sendtTidspunkt)
    }
}
