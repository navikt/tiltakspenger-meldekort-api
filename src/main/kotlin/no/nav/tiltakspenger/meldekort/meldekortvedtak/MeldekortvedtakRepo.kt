package no.nav.tiltakspenger.meldekort.meldekortvedtak

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

/**
 * Lesesiden for meldekortvedtak (papirmeldekort / saksbehandler-behandlede meldeperioder).
 *
 * Skrivesiden (INSERT) bor i [no.nav.tiltakspenger.meldekort.mottak.MottakRepo.lagreMeldekortvedtak].
 */
interface MeldekortvedtakRepo {
    /** Alle meldekortvedtak for [sakId], sortert på opprettet stigende. */
    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): List<Meldekortvedtak>
}
