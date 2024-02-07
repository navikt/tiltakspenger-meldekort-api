package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBehandling

interface Utbetaling {
    suspend fun sendTilUtbetaling(behandling: MeldekortBehandling): String
}
