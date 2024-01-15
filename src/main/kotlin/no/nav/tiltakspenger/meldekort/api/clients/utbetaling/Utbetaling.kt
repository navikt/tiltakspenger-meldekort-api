package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort

interface Utbetaling {
    suspend fun sendTilUtbetaling(meldekort: Meldekort.Innsendt): String
}
