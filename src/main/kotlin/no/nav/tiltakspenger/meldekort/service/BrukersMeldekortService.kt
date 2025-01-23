package no.nav.tiltakspenger.meldekort.service

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraUtfylling
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.repository.BrukersMeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import java.time.LocalDateTime

class BrukersMeldekortService(
    val brukersMeldekortRepo: BrukersMeldekortRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
) {
    private val logger = KotlinLogging.logger { }

    fun lagreBrukersMeldekort(meldekort: MeldekortFraUtfylling) {
        if (brukersMeldekortRepo.hentMeldekortForMeldeperiodeKjedeId(meldekort.meldeperiodeKjedeId) != null) {
            throw IllegalArgumentException("Innsending med id ${meldekort.meldeperiodeId} finnes allerede")
        }
        val meldeperiode = meldeperiodeRepo.hentForId(meldekort.meldeperiodeId)!!
        val brukersMeldekort = BrukersMeldekort(
            id = MeldekortId.random(),
            mottatt = LocalDateTime.now(),
            meldeperiode = meldeperiode,
            sakId = meldeperiode.sakId,
            dager = meldekort.meldekortDager,

        )
        brukersMeldekortRepo.lagre(brukersMeldekort)
    }

    fun hentMeldekortForMeldeperiodeKjedeId(id: String): BrukersMeldekort? {
        return brukersMeldekortRepo.hentMeldekortForMeldeperiodeKjedeId(id)
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

    fun markerSendt(
        id: MeldekortId,
        meldekortStatus: MeldekortStatus,
        innsendtTidspunkt: LocalDateTime,
    ) {
        brukersMeldekortRepo.markerSendt(id, meldekortStatus, innsendtTidspunkt)
    }
}
