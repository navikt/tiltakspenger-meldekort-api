package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortForKjede
import no.nav.tiltakspenger.meldekort.domene.MeldekortMedSisteMeldeperiode
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.Clock
import java.time.LocalDateTime

class MeldekortRepoFake(
    private val clock: Clock,
) : MeldekortRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, Meldekort>())

    /** Sporer sendtTilSaksbehandling-tidspunkt, da dette kun er et DB-felt og ikke del av domenemodellen. */
    private val sendtTilSaksbehandling = Atomic(mutableMapOf<MeldekortId, LocalDateTime>())

    override fun lagre(meldekort: Meldekort, sessionContext: SessionContext?) {
        data.get()[meldekort.id] = meldekort
    }

    override fun deaktiver(
        meldekortId: MeldekortId,
        sessionContext: SessionContext?,
    ) {
        val meldekort = data.get()[meldekortId]
        requireNotNull(meldekort) {
            "Kan ikke deaktivere meldekort $meldekortId - meldekortet finnes ikke"
        }

        data.get()[meldekortId] = meldekort.copy(
            deaktivert = nå(clock),
        )
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
            .filter { it.fnr == fnr && it.erInnsendt }
            .sortedBy { it.mottatt }
            .lastOrNull()
    }

    override fun hentInnsendteMeldekortForBruker(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): List<MeldekortMedSisteMeldeperiode> {
        return data.get().values
            .filter { it.fnr == fnr && it.mottatt != null && it.deaktivert == null }
            .sortedWith(compareByDescending<Meldekort> { it.periode.fraOgMed }.thenByDescending { it.meldeperiode.versjon })
            .map { meldekort ->
                val sisteMeldeperiode = data.get().values
                    .filter { it.sakId == meldekort.sakId && it.meldeperiode.kjedeId == meldekort.meldeperiode.kjedeId }
                    .maxByOrNull { it.meldeperiode.versjon }
                    ?.meldeperiode
                    ?: meldekort.meldeperiode
                MeldekortMedSisteMeldeperiode(meldekort, sisteMeldeperiode)
            }
    }

    override fun hentMeldekortForSendingTilSaksbehandling(sessionContext: SessionContext?): List<Meldekort> {
        return data.get().values
            .filter { it.mottatt != null && it.journalpostId != null && sendtTilSaksbehandling.get()[it.id] == null }
            .sortedByDescending { it.mottatt }
    }

    override fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        sendtTilSaksbehandling.get()[id] = sendtTidspunkt
    }

    override fun markerJournalført(
        meldekortId: MeldekortId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        val meldekort = data.get()[meldekortId]
        requireNotNull(meldekort) {
            "Kan ikke markere journalført for meldekort $meldekortId - meldekortet finnes ikke"
        }
        data.get()[meldekortId] = meldekort.copy(journalpostId = journalpostId, journalføringstidspunkt = tidspunkt)
    }

    override fun hentDeSomSkalJournalføres(limit: Int, sessionContext: SessionContext?): List<Meldekort> {
        return data.get().values
            .filter { it.mottatt != null && it.journalpostId == null && it.saksnummer.isNotBlank() }
            .take(limit)
    }

    override fun hentSisteMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Meldekort? {
        return data.get().values
            .filter { it.meldeperiode.kjedeId == kjedeId && it.fnr == fnr }
            .maxByOrNull { it.meldeperiode.versjon }
    }

    override fun hentAlleMeldekortKlarTilInnsending(fnr: Fnr, sessionContext: SessionContext?): List<Meldekort> {
        return data.get().values
            .filter { it.fnr == fnr && it.klarTilInnsending(clock) }
            .sortedBy { it.meldeperiode.periode.fraOgMed }
    }

    fun hentAlleForSakId(sakId: no.nav.tiltakspenger.libs.common.SakId): List<Meldekort> {
        return data.get().values.filter { it.sakId == sakId }
    }
}
