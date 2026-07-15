package no.nav.tiltakspenger.meldekort.bruker

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

/**
 * Read-only repo for å hente [SakForBruker] for brukerflyten.
 *
 * Egne spørringer og egen datatype slik at vi ikke deler `Sak`-aggregatet på tvers av formål med ulike krav til relaterte data (meldeperioder, meldekortvedtak).
 */
interface BrukerSakRepo {
    fun hentForBruker(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): SakForBruker?
}
