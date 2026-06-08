package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.journalføring.JournalføringRepo
import no.nav.tiltakspenger.meldekort.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortForKjede
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortMedSisteMeldeperiode
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortRepo
import no.nav.tiltakspenger.meldekort.sending.SendMeldekortRepo
import java.time.Clock
import java.time.LocalDateTime

/**
 * In-memory fake som modellerer `meldekort_bruker`-tabellen. Den implementerer flere repo-porter
 * ([MeldekortRepo], [SendMeldekortRepo], [JournalføringRepo]) over samme backing-store, slik at
 * tester ser konsistente data uavhengig av hvilket domene-repo de går via – akkurat som mot én DB.
 */
class MeldekortRepoFake(
    private val clock: Clock,
    private val meldekortvedtakRepoFake: MeldekortvedtakRepoFake = MeldekortvedtakRepoFake(),
) : MeldekortRepo,
    SendMeldekortRepo,
    JournalføringRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, BrukersMeldekort>())

    /** Sporer sendtTilSaksbehandling-tidspunkt, da dette kun er et DB-felt og ikke del av domenemodellen. */
    private val sendtTilSaksbehandling = Atomic(mutableMapOf<MeldekortId, LocalDateTime>())

    override fun lagre(meldekort: BrukersMeldekort, sessionContext: SessionContext?) {
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

    override fun hentForMeldekortId(meldekortId: MeldekortId, fnr: Fnr, sessionContext: SessionContext?): BrukersMeldekort? {
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

    override fun hentNesteMeldekortTilUtfylling(fnr: Fnr, sessionContext: SessionContext?): BrukersMeldekort? {
        return data.get().values.filter {
            it.fnr == fnr && it.deaktivert == null && it.mottatt == null && !harMeldekortvedtakForKjede(it)
        }
            .sortedWith(compareBy<BrukersMeldekort> { it.periode.fraOgMed }.thenByDescending { it.meldeperiode.versjon })
            .firstOrNull()
    }

    override fun hentSisteUtfylteMeldekort(fnr: Fnr, sessionContext: SessionContext?): BrukersMeldekort? {
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
            .sortedWith(compareByDescending<BrukersMeldekort> { it.periode.fraOgMed }.thenByDescending { it.meldeperiode.versjon })
            .map { meldekort ->
                val sisteMeldeperiode = data.get().values
                    .filter { it.sakId == meldekort.sakId && it.meldeperiode.kjedeId == meldekort.meldeperiode.kjedeId }
                    .maxByOrNull { it.meldeperiode.versjon }
                    ?.meldeperiode
                    ?: meldekort.meldeperiode
                MeldekortMedSisteMeldeperiode(meldekort, sisteMeldeperiode)
            }
    }

    override fun hentMeldekortForSendingTilSaksbehandling(sessionContext: SessionContext?): List<BrukersMeldekort> {
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

    override fun hentDeSomSkalJournalføres(limit: Int, sessionContext: SessionContext?): List<BrukersMeldekort> {
        return data.get().values
            .filter { it.mottatt != null && it.journalpostId == null && it.saksnummer.isNotBlank() }
            .take(limit)
    }

    override fun hentSisteMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): BrukersMeldekort? {
        return data.get().values
            .filter { it.meldeperiode.kjedeId == kjedeId && it.fnr == fnr }
            .maxByOrNull { it.meldeperiode.versjon }
    }

    override fun hentAlleMeldekortKlarTilInnsending(fnr: Fnr, sessionContext: SessionContext?): List<BrukersMeldekort> {
        return data.get().values
            .filter { it.fnr == fnr && it.klarTilInnsending(clock) && !harMeldekortvedtakForKjede(it) }
            .sortedBy { it.meldeperiode.periode.fraOgMed }
    }

    fun hentAlleForSakId(sakId: no.nav.tiltakspenger.libs.common.SakId): List<BrukersMeldekort> {
        return data.get().values.filter { it.sakId == sakId }
    }

    /**
     * Speiler ekskluderingen i [no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortPostgresRepo]:
     * En kjede mangler ikke innsending dersom det finnes et meldekortvedtak (f.eks. papirmeldekort) for kjeden.
     */
    private fun harMeldekortvedtakForKjede(meldekort: BrukersMeldekort): Boolean {
        return meldekortvedtakRepoFake.hentForSakId(meldekort.sakId).any { vedtak ->
            vedtak.meldeperiodebehandlinger.any { it.meldeperiodeKjedeId == meldekort.meldeperiode.kjedeId }
        }
    }
}
