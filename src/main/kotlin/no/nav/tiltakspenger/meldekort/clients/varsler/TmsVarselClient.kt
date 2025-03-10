package no.nav.tiltakspenger.meldekort.clients.varsler

import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.VarselId

interface TmsVarselClient {
    fun sendVarselForNyttMeldekort(meldekort: Meldekort, varselId: VarselId)
    fun inaktiverVarsel(varselId: VarselId)
}
