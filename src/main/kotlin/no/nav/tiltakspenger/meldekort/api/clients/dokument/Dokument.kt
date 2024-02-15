package no.nav.tiltakspenger.meldekort.api.clients.dokument
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag

interface Dokument {
    suspend fun sendMeldekortTilDokument(meldekort: Meldekort?, grunnlag: MeldekortGrunnlag): JoarkResponse
}
