package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning

interface Utbetaling {
    suspend fun sendTilUtbetaling(behandling: MeldekortBeregning): String
}
