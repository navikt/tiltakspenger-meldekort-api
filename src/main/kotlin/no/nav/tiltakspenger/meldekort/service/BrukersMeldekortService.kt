package no.nav.tiltakspenger.meldekort.service

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraUtfylling
import no.nav.tiltakspenger.meldekort.repository.BrukersMeldekortRepo
import java.time.LocalDateTime

class BrukersMeldekortService(
    val brukersMeldekortRepo: BrukersMeldekortRepo,
) {
    private val logger = KotlinLogging.logger { }

    // Burde vi validere at det innsendte meldekort faktiske tilhører brukeren som sendte inn?
    fun lagreBrukersMeldekort(meldekort: MeldekortFraUtfylling, fnr: Fnr) {
        val brukersMeldekort = brukersMeldekortRepo.hentForMeldekortId(meldekort.id)
            ?: throw IllegalArgumentException("Meldekortet med id ${meldekort.id} finnes ikke")

        if (brukersMeldekort.mottatt != null) {
            throw IllegalArgumentException("Meldekortet med id ${meldekort.id} er allerede mottatt")
        }

        brukersMeldekortRepo.lagreUtfylling(meldekort)
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
