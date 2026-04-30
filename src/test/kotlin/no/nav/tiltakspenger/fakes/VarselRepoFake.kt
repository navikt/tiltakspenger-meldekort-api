package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsler
import no.nav.tiltakspenger.meldekort.repository.VarselRepo
import java.time.Clock

class VarselRepoFake(
    private val clock: Clock,
) : VarselRepo {
    private val data = Atomic(mutableMapOf<VarselId, Varsel>())

    override fun lagre(
        varsel: Varsel,
        aktiveringsmetadata: String?,
        inaktiveringsmetadata: String?,
        sessionContext: SessionContext?,
    ) {
        data.get()[varsel.varselId] = varsel
    }

    override fun hentVarslerForSakId(sakId: SakId, sessionContext: SessionContext?): Varsler {
        return Varsler(
            data.get().values.filter { it.sakId == sakId }.sortedBy { it.opprettet },
        )
    }

    override fun hentSakerMedVarslerSomSkalAktiveres(limit: Int, sessionContext: SessionContext?): List<SakId> {
        val now = nå(clock)
        return data.get().values
            .filterIsInstance<Varsel.SkalAktiveres>()
            .filter { it.skalAktiveresTidspunkt <= now }
            .sortedBy { it.opprettet }
            .map { it.sakId }
            .distinct()
            .take(limit)
    }

    override fun hentSakerMedVarslerSomSkalInaktiveres(limit: Int, sessionContext: SessionContext?): List<SakId> {
        val now = nå(clock)
        return data.get().values
            .filterIsInstance<Varsel.SkalInaktiveres>()
            .filter { it.skalInaktiveresTidspunkt <= now }
            .sortedBy { it.skalInaktiveresTidspunkt }
            .map { it.sakId }
            .distinct()
            .take(limit)
    }
}
