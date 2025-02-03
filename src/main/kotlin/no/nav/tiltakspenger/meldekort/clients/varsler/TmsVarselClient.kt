package no.nav.tiltakspenger.meldekort.clients.varsler

import no.nav.tiltakspenger.meldekort.domene.Meldekort

interface TmsVarselClient {
    fun sendVarselForNyttMeldekort(meldekort: Meldekort, eventId: String)
}
