package no.nav.tiltakspenger.meldekort.meldekortvedtak

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

interface MeldekortvedtakRepo {
    fun lagre(meldekortvedtak: Meldekortvedtak, sessionContext: SessionContext?)
}
