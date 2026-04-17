package no.nav.tiltakspenger.meldekort.clients.varsler

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.domene.VarselId

interface VarselClient {
    fun sendVarsel(varselId: VarselId, fnr: Fnr): SendtVarselMetadata
    fun inaktiverVarsel(varselId: VarselId)
}
