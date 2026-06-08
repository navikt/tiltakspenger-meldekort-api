package no.nav.tiltakspenger.meldekort.sending

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import java.time.LocalDateTime

interface SendMeldekortRepo {
    fun hentMeldekortForSendingTilSaksbehandling(sessionContext: SessionContext? = null): List<BrukersMeldekort>

    fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
        sessionContext: SessionContext? = null,
    )
}
