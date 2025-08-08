// så intellij ikke fjerner Atomic fra optimize imports
@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.fakes

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortForKjede
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.Clock
import java.time.LocalDateTime
import kotlin.Boolean
import kotlin.Int
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.lastOrNull
import kotlin.collections.map
import kotlin.collections.maxByOrNull
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.sortedBy
import kotlin.collections.sortedWith
import kotlin.let
import kotlin.requireNotNull

class MeldekortRepoFake(
    private val clock: Clock,
) : MeldekortRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, Meldekort>())

    override fun opprett(meldekort: Meldekort, sessionContext: SessionContext?) {
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

    override fun hentMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): MeldekortForKjede {
        return data.get().values.filter { it.fnr == fnr && it.meldeperiode.kjedeId == kjedeId }
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

    override fun hentAlleMeldekortForBruker(fnr: Fnr, limit: Int, sessionContext: SessionContext?): List<Meldekort> {
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

    override fun hentSisteMeldeperiodeForMeldeperiodeKjedeId(
        id: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Meldeperiode? {
        return data.get()
            .filter { it.value.meldeperiode.kjedeId == id && it.value.fnr == fnr }
            .values
            .maxByOrNull { it.meldeperiode.versjon }?.meldeperiode
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

    override fun hentMeldeperiodeForPeriode(
        periode: Periode,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Meldeperiode? {
        return data.get().filter { it.value.meldeperiode.periode == periode }.values.lastOrNull()?.meldeperiode
    }
}
