package no.nav.tiltakspenger.meldekort.clients.varsler

import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort

interface TmsVarselClient {
    fun sendVarselForNyttMeldekort(meldekort: BrukersMeldekort, eventId: String)
}
