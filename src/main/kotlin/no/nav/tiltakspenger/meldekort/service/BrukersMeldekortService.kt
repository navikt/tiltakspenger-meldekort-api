package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.validerLagring
import no.nav.tiltakspenger.meldekort.repository.BrukersMeldekortRepo
import java.time.LocalDateTime

class BrukersMeldekortService(
    val brukersMeldekortRepo: BrukersMeldekortRepo,
) {
    fun lagreBrukersMeldekort(lagreMeldekortKommando: LagreMeldekortFraBrukerKommando, innsenderFnr: Fnr) {
        val innsendtMeldekortId = lagreMeldekortKommando.id

        val brukersMeldekort = brukersMeldekortRepo.hentForMeldekortId(innsendtMeldekortId)

        require(brukersMeldekort != null) {
            "Meldekortet med id $innsendtMeldekortId finnes ikke"
        }

        require(brukersMeldekort.mottatt != null) {
            "Meldekortet med id $innsendtMeldekortId er allerede mottatt"
        }

        if (brukersMeldekort.fnr != innsenderFnr) {
            sikkerlogg.error {
                "Meldekortet med id $innsendtMeldekortId ble innsendt av ${innsenderFnr.verdi} men tilhører ${brukersMeldekort.fnr}"
            }
            throw IllegalArgumentException("Meldekortet med id $innsendtMeldekortId ble innsendt av feil bruker")
        }

        brukersMeldekort.validerLagring(lagreMeldekortKommando).also {
            brukersMeldekortRepo.lagreUtfylling(lagreMeldekortKommando)
        }
    }

    fun hentMeldekortForMeldeperiodeKjedeId(id: String): BrukersMeldekort? {
        return brukersMeldekortRepo.hentMeldekortForMeldeperiodeKjedeId(id)
    }

    fun hentForMeldekortId(id: MeldekortId): BrukersMeldekort? {
        return brukersMeldekortRepo.hentForMeldekortId(id)
    }

    fun hentSisteMeldekort(fnr: Fnr): BrukersMeldekort? {
        return brukersMeldekortRepo.hentSisteMeldekort(fnr)
    }

    fun hentAlleMeldekort(fnr: Fnr): List<BrukersMeldekort> {
        return brukersMeldekortRepo.hentAlleMeldekort(fnr)
    }

    fun hentMeldekortSomSkalSendesTilSaksbehandling(): List<BrukersMeldekort> {
        return brukersMeldekortRepo.hentUsendteMeldekort()
    }

    fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
    ) {
        brukersMeldekortRepo.markerSendtTilSaksbehandling(id, sendtTidspunkt)
    }
}
