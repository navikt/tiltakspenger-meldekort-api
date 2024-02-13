package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning

interface Utbetaling {
    suspend fun sendTilUtbetaling(sakId: String, behandling: MeldekortBeregning): String
}
