package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import java.time.LocalDateTime

interface MeldekortRepo {
    fun lagre(
        meldekort: Meldekort,
        sessionContext: SessionContext? = null,
    )

    fun lagreFraBruker(
        meldekort: LagreMeldekortFraBrukerKommando,
        sessionContext: SessionContext? = null,
    )

    fun hentForMeldekortId(
        meldekortId: MeldekortId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldekort?

    fun hentMeldekortForMeldeperiodeId(
        meldeperiodeId: String,
        sessionContext: SessionContext? = null,
    ): Meldekort?

    fun hentMeldekortForMeldeperiodeKjedeId(
        meldeperiodeKjedeId: String,
        sessionContext: SessionContext? = null,
    ): Meldekort?

    fun hentSisteMeldekort(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldekort?

    fun hentAlleMeldekort(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): List<Meldekort>

    fun hentUsendteMeldekort(sessionContext: SessionContext? = null): List<Meldekort>

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

    fun hentMottatteSomDetVarslesFor(limit: Int = 25, sessionContext: SessionContext? = null): List<Meldekort>
}
