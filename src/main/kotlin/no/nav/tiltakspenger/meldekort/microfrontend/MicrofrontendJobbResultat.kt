package no.nav.tiltakspenger.meldekort.microfrontend

import no.nav.tiltakspenger.libs.common.SakId

/**
 * Utfallet av et microfrontend-jobbkjør – enten for én enkelt sak eller aggregert for flere.
 * Skiller på saker som ble håndtert vellykket og saker som feilet (feilet svelges og prøves på nytt
 * ved neste jobbkjøring), slik at både drift og tester kan forholde seg til resultatet eksplisitt.
 *
 * @param vellykkede Saker hvor aktivering/inaktivering gikk bra.
 * @param feilede Saker hvor kallet feilet.
 */
data class MicrofrontendJobbResultat(
    val vellykkede: List<SakId>,
    val feilede: List<SakId>,
) {
    companion object {
        val tom = MicrofrontendJobbResultat(vellykkede = emptyList(), feilede = emptyList())
    }
}
