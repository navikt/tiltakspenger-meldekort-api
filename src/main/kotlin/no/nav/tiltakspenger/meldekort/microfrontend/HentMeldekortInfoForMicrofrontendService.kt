package no.nav.tiltakspenger.meldekort.microfrontend

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.meldekort.HentMeldekortService
import java.time.Clock
import java.time.LocalDateTime

class HentMeldekortInfoForMicrofrontendService(
    private val hentMeldekortService: HentMeldekortService,
    private val clock: Clock,
) {
    fun hentInformasjonOmMeldekortForMicrofrontend(fnr: Fnr): Pair<Int, LocalDateTime?> {
        val antallMeldekortKlarTilInnsending = hentMeldekortService.hentAlleMeldekortKlarTilInnsending(fnr).size
        val nesteMuligeInnsending =
            hentMeldekortService.hentNesteMeldekortForUtfylling(fnr)?.klarTilInnsendingDateTime(clock)

        return Pair(antallMeldekortKlarTilInnsending, nesteMuligeInnsending)
    }
}
