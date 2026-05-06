package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.varsler.BeskjedVarsel

interface BeskjedVarselRepo {
    fun lagre(
        beskjedVarsel: BeskjedVarsel,
        sendingsmetadata: String,
        sessionContext: SessionContext? = null,
    )

    fun hentAntallBeskjederForSak(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): Int
}
