package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.meldekort.repository.SakVarselRepo
import java.time.LocalDateTime

class SakVarselRepoFake(
    private val sakRepoFake: SakRepoFake,
) : SakVarselRepo {
    private val flaggedForVurdering = Atomic(mutableSetOf<SakId>())
    private val vurdertTidspunkt = Atomic(mutableMapOf<SakId, LocalDateTime>())

    override fun flaggForVarselvurdering(sakId: SakId, sessionContext: SessionContext?) {
        flaggedForVurdering.get().add(sakId)
    }

    override fun hentSakerSomSkalVurdereVarsel(limit: Int, sessionContext: SessionContext?): List<Sak> {
        return flaggedForVurdering.get()
            .mapNotNull { sakId -> sakRepoFake.hent(sakId) }
            .take(limit)
    }

    override fun markerVarselVurdert(sakId: SakId, vurdertTidspunkt: LocalDateTime, sessionContext: SessionContext?) {
        flaggedForVurdering.get().remove(sakId)
        this.vurdertTidspunkt.get()[sakId] = vurdertTidspunkt
    }
}
