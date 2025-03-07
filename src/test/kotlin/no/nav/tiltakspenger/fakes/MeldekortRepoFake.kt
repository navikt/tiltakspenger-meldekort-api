package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.domene.senesteTilOgMedDatoForInnsending
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.LocalDateTime

class MeldekortRepoFake : MeldekortRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, Meldekort>())

    override fun lagre(meldekort: Meldekort, sessionContext: SessionContext?) {
        data.get()[meldekort.id] = meldekort
    }

    override fun lagreFraBruker(lagreKommando: LagreMeldekortFraBrukerKommando, sessionContext: SessionContext?) {
        val meldekort = hentForMeldekortId(lagreKommando.id, lagreKommando.fnr, sessionContext)

        requireNotNull(meldekort) { "Kan ikke lagre meldekort ${lagreKommando.id} fra bruker ${lagreKommando.fnr} - meldekortet finnes ikke" }

        data.get()[meldekort.id] = meldekort.copy(
            dager = lagreKommando.dager.map { it.tilMeldekortDag() },
            mottatt = lagreKommando.mottatt,
        )
    }

    override fun oppdater(meldekort: Meldekort, sessionContext: SessionContext?) {
        data.get()[meldekort.id] = meldekort
    }

    override fun hentForMeldekortId(meldekortId: MeldekortId, fnr: Fnr, sessionContext: SessionContext?): Meldekort? {
        return data.get()[meldekortId]?.let { if (it.fnr == fnr) it else null }
    }

    override fun hentMeldekortForMeldeperiodeId(
        meldeperiodeId: MeldeperiodeId,
        sessionContext: SessionContext?,
    ): Meldekort? {
        return data.get().values.find { it.meldeperiode.id == meldeperiodeId }
    }

    override fun hentMeldekortForMeldeperiodeKjedeId(
        meldeperiodeKjedeId: MeldeperiodeKjedeId,
        sessionContext: SessionContext?,
    ): Meldekort? {
        return data.get().values.find { it.meldeperiode.kjedeId == meldeperiodeKjedeId }
    }

    override fun hentSisteMeldekortForBruker(fnr: Fnr, sessionContext: SessionContext?): Meldekort? {
        return data.get().values
            .filter { it.fnr == fnr && it.meldeperiode.periode.tilOgMed <= senesteTilOgMedDatoForInnsending() }
            .maxByOrNull { it.meldeperiode.periode.fraOgMed }
    }

    override fun hentNesteMeldekortTilUtfylling(fnr: Fnr, sessionContext: SessionContext?): Meldekort? {
        return data.get().values
            .filter { it.fnr == fnr && it.meldeperiode.periode.tilOgMed <= senesteTilOgMedDatoForInnsending() && it.mottatt == null }
            .minByOrNull { it.meldeperiode.periode.fraOgMed }
    }

    override fun hentAlleMeldekortForBruker(fnr: Fnr, sessionContext: SessionContext?): List<Meldekort> {
        return data.get().values
            .filter { it.fnr == fnr }
            .sortedByDescending { it.meldeperiode.periode.fraOgMed }
    }

    override fun hentMeldekortForSendingTilSaksbehandling(sessionContext: SessionContext?): List<Meldekort> {
        TODO("Not yet implemented")
    }

    override fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        TODO("Not yet implemented")
    }

    override fun markerJournalført(
        meldekortId: MeldekortId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        TODO("Not yet implemented")
    }

    override fun hentDeSomSkalJournalføres(limit: Int, sessionContext: SessionContext?): List<Meldekort> {
        TODO("Not yet implemented")
    }

    override fun hentDeSomIkkeHarBlittVarsletFor(limit: Int, sessionContext: SessionContext?): List<Meldekort> {
        TODO("Not yet implemented")
    }

    override fun hentMottatteSomDetVarslesFor(limit: Int, sessionContext: SessionContext?): List<Meldekort> {
        TODO("Not yet implemented")
    }
}
