package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.validerLagring
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.LocalDateTime

class MeldekortService(
    val meldekortRepo: MeldekortRepo,
) {
    fun lagreMeldekortFraBruker(lagreKommando: LagreMeldekortFraBrukerKommando, innsenderFnr: Fnr) {
        val meldekortId = lagreKommando.id
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId)

        requireNotNull(meldekort) {
            "Meldekortet med id $meldekortId finnes ikke"
        }

        require(meldekort.mottatt == null) {
            "Meldekortet med id $meldekortId er allerede mottatt"
        }

        if (meldekort.fnr != innsenderFnr) {
            sikkerlogg.error {
                "Meldekortet med id $meldekortId ble innsendt av ${innsenderFnr.verdi} men tilhører ${meldekort.fnr}"
            }
            throw IllegalArgumentException("Meldekortet med id $meldekortId ble innsendt av feil bruker")
        }

        meldekort.validerLagring(lagreKommando).also {
            meldekortRepo.lagreFraBruker(lagreKommando)
        }
    }

    fun hentMeldekortForMeldeperiodeKjedeId(id: String): Meldekort? {
        return meldekortRepo.hentMeldekortForMeldeperiodeKjedeId(id)
    }

    fun hentForMeldekortId(id: MeldekortId): Meldekort? {
        return meldekortRepo.hentForMeldekortId(id)
    }

    fun hentSisteMeldekort(fnr: Fnr): Meldekort? {
        return meldekortRepo.hentSisteMeldekort(fnr)
    }

    fun hentAlleMeldekort(fnr: Fnr): List<Meldekort> {
        return meldekortRepo.hentAlleMeldekort(fnr)
    }

    fun hentMeldekortSomSkalSendesTilSaksbehandling(): List<Meldekort> {
        return meldekortRepo.hentUsendteMeldekort()
    }

    fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
    ) {
        meldekortRepo.markerSendtTilSaksbehandling(id, sendtTidspunkt)
    }
}
