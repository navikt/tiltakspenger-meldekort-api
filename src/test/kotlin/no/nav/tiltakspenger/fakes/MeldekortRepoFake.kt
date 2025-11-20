package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortForKjede
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.Clock
import java.time.LocalDateTime

class MeldekortRepoFake(
    private val clock: Clock,
) : MeldekortRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, Meldekort>())

    override fun lagre(meldekort: Meldekort, sessionContext: SessionContext?) {
        data.get()[meldekort.id] = meldekort
    }

    override fun deaktiver(
        meldekortId: MeldekortId,
        deaktiverVarsel: Boolean,
        sessionContext: SessionContext?,
    ) {
        val meldekort = data.get()[meldekortId]
        requireNotNull(meldekort) {
            "Kan ikke deaktivere meldekort $meldekortId - meldekortet finnes ikke"
        }

        data.get()[meldekortId] = meldekort.copy(deaktivert = nå(clock), erVarselInaktivert = !deaktiverVarsel)
    }

    override fun hentForMeldekortId(meldekortId: MeldekortId, fnr: Fnr, sessionContext: SessionContext?): Meldekort? {
        return data.get()[meldekortId]?.let { if (it.fnr == fnr) it else null }
    }

    override fun hentMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): MeldekortForKjede {
        return data.get().values.filter { it.fnr == fnr && it.meldeperiode.kjedeId == kjedeId }
            .sortedWith(compareBy({ it.meldeperiode.versjon }, { it.mottatt }))
            .let { MeldekortForKjede(it) }
    }

    override fun hentNesteMeldekortTilUtfylling(fnr: Fnr, sessionContext: SessionContext?): Meldekort? {
        return data.get().values.filter {
            it.fnr == fnr && it.deaktivert == null && it.mottatt == null
        }
            .sortedWith(compareBy<Meldekort> { it.periode.fraOgMed }.thenByDescending { it.meldeperiode.versjon })
            .firstOrNull()
    }

    override fun hentSisteUtfylteMeldekort(fnr: Fnr, sessionContext: SessionContext?): Meldekort? {
        return data.get().values
            .filter { it.fnr == fnr && it.mottatt != null }
            .sortedBy { it.mottatt }
            .lastOrNull()
    }

    override fun hentInnsendteMeldekortForBruker(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): List<Meldekort> {
        return data.get().values
            .filter {
                it.fnr == fnr && it.periode.tilOgMed <= Meldekort.senesteTilOgMedDatoForInnsending() && it.deaktivert == null
            }
            .sortedWith(compareByDescending<Meldekort> { it.periode.fraOgMed }.thenByDescending { it.meldeperiode.versjon })
    }

    override fun hentMeldekortForSendingTilSaksbehandling(sessionContext: SessionContext?): List<Meldekort> {
        return emptyList()
    }

    override fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
    }

    override fun markerJournalført(
        meldekortId: MeldekortId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
    }

    override fun hentDeSomSkalJournalføres(limit: Int, sessionContext: SessionContext?): List<Meldekort> {
        return emptyList()
    }

    override fun hentMeldekortDetSkalVarslesFor(limit: Int, sessionContext: SessionContext?): List<Meldekort> {
        return emptyList()
    }

    override fun hentMottatteEllerDeaktiverteSomDetVarslesFor(
        limit: Int,
        sessionContext: SessionContext?,
    ): List<Meldekort> {
        return emptyList()
    }

    override fun hentSisteMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Meldekort? {
        return data.get()
            .filter { it.value.meldeperiode.kjedeId == kjedeId && it.value.fnr == fnr }
            .maxByOrNull { it.value.meldeperiode.versjon }
            ?.value
    }

    override fun hentAlleMeldekortKlarTilInnsending(fnr: Fnr, sessionContext: SessionContext?): List<Meldekort> {
        return data.get()
            .values
            .toList()
    }
}
