package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraUtfylling
import java.time.LocalDateTime

interface BrukersMeldekortRepo {
    fun lagre(
        meldekort: BrukersMeldekort,
        sessionContext: SessionContext? = null,
    )

    fun lagreUtfylling(
        meldekort: MeldekortFraUtfylling,
        sessionContext: SessionContext? = null,
    )

    fun hentForMeldekortId(
        meldekortId: MeldekortId,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentMeldekortForMeldeperiodeId(
        meldeperiodeId: String,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentMeldekortForMeldeperiodeKjedeId(
        meldeperiodeKjedeId: String,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentSisteMeldekort(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentAlleMeldekort(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun hentUsendteMeldekort(sessionContext: SessionContext? = null): List<BrukersMeldekort>

    fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
        sessionContext: SessionContext? = null,
    )
}
