package no.nav.tiltakspenger.meldekort.microfrontend

import no.nav.tiltakspenger.libs.common.SakId

/**
 * Utfallet av en microfrontend-jobbkjøring – enten for én enkelt sak eller aggregert for flere.
 * Skiller mellom saker som ble håndtert vellykket og saker som feilet (feilede saker svelges og prøves på nytt ved neste jobbkjøring), slik at både drift og tester kan forholde seg til resultatet eksplisitt.
 *
 * @param vellykkede Saker hvor aktivering/inaktivering gikk bra.
 * @param feilede Saker hvor kallet feilet.
 * @param kunneIkkeHenteSaker Om selve hentingen av saker feilet, slik at jobben ikke fikk gjort noe – skilles fra et tomt resultat så en feilet kjøring ikke rapporteres som «ingen arbeid».
 */
data class MicrofrontendJobbResultat(
    val vellykkede: List<SakId>,
    val feilede: List<SakId>,
    val kunneIkkeHenteSaker: Boolean,
) {
    companion object {
        val henteFeil = MicrofrontendJobbResultat(vellykkede = emptyList(), feilede = emptyList(), kunneIkkeHenteSaker = true)
    }
}
