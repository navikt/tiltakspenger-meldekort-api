package no.nav.tiltakspenger.meldekort.journalføring

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import java.time.LocalDateTime

interface JournalføringRepo {
    fun hentDeSomSkalJournalføres(limit: Int = 10, sessionContext: SessionContext? = null): List<BrukersMeldekort>

    fun markerJournalført(
        meldekortId: MeldekortId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext? = null,
    )
}
