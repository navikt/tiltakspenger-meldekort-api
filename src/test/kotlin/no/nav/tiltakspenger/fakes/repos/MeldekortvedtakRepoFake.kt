package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak

class MeldekortvedtakRepoFake {
    private val data = Atomic(mutableMapOf<VedtakId, Meldekortvedtak>())

    fun lagre(meldekortvedtak: Meldekortvedtak, sessionContext: SessionContext? = null) {
        data.get()[meldekortvedtak.id] = meldekortvedtak
    }

    /**
     * Speiler [no.nav.tiltakspenger.meldekort.meldekortvedtak.infra.MeldekortvedtakPostgresRepo.hentForSakId]:
     * returnerer alle meldekortvedtak for sakId sortert på opprettet stigende.
     */
    fun hentForSakId(sakId: SakId): List<Meldekortvedtak> =
        data.get().values
            .filter { it.sakId == sakId }
            .sortedBy { it.opprettet }
}
