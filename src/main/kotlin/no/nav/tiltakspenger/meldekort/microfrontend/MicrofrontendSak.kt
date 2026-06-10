package no.nav.tiltakspenger.meldekort.microfrontend

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId

/**
 * Datamodellen microfrontend-pakken trenger for å aktivere/inaktivere microfrontend for en bruker.
 * Eies av microfrontend-pakken; ikke knyttet til [no.nav.tiltakspenger.meldekort.sak.Sak].
 */
data class MicrofrontendSak(
    val sakId: SakId,
    val fnr: Fnr,
)
