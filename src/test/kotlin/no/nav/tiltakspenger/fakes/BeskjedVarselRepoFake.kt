package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.varsler.BeskjedVarsel
import no.nav.tiltakspenger.meldekort.repository.BeskjedVarselRepo

class BeskjedVarselRepoFake : BeskjedVarselRepo {
    private val data = Atomic(mutableListOf<BeskjedVarsel>())

    override fun lagre(
        beskjedVarsel: BeskjedVarsel,
        sendingsmetadata: String,
        sessionContext: SessionContext?,
    ) {
        data.get().add(beskjedVarsel)
    }

    override fun hentAntallBeskjederForSak(sakId: SakId, sessionContext: SessionContext?): Int {
        return data.get().count { it.sakId == sakId }
    }

    fun hentAlle(): List<BeskjedVarsel> = data.get().toList()
}
