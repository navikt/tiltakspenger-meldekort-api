package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import java.time.LocalDateTime

interface MeldekortRepo {
    fun opprett(
        meldekort: Meldekort,
        sessionContext: SessionContext? = null,
    )

    fun deaktiver(
        meldekortId: MeldekortId,
        deaktiverVarsel: Boolean,
        sessionContext: SessionContext? = null,
    )

    fun lagreFraBruker(
        lagreKommando: LagreMeldekortFraBrukerKommando,
        sessionContext: SessionContext? = null,
    )

    fun oppdater(
        meldekort: Meldekort,
        sessionContext: SessionContext? = null,
    )

    fun hentForMeldekortId(
        meldekortId: MeldekortId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldekort?

    fun hentMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): List<Meldekort>

    fun hentSisteUtfylteMeldekort(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldekort?

    fun hentNesteMeldekortTilUtfylling(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldekort?

    fun hentAlleMeldekortForBruker(
        fnr: Fnr,
        limit: Int = 100,
        sessionContext: SessionContext? = null,
    ): List<Meldekort>

    fun hentMeldekortForSendingTilSaksbehandling(sessionContext: SessionContext? = null): List<Meldekort>

    fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
        sessionContext: SessionContext? = null,
    )

    fun markerJournalført(
        meldekortId: MeldekortId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext? = null,
    )

    fun hentDeSomSkalJournalføres(limit: Int = 10, sessionContext: SessionContext? = null): List<Meldekort>

    fun hentMeldekortDetSkalVarslesFor(limit: Int = 25, sessionContext: SessionContext? = null): List<Meldekort>

    fun hentMottatteEllerDeaktiverteSomDetVarslesFor(
        limit: Int = 25,
        sessionContext: SessionContext? = null,
    ): List<Meldekort>

    fun hentSisteMeldeperiodeForMeldeperiodeKjedeId(
        id: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldeperiode?

    fun hentSisteMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldekort?
}
