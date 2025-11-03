package no.nav.tiltakspenger

import no.nav.tiltakspenger.fakes.TexasClientFake
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.meldekort.clients.microfrontend.TmsMikrofrontendClientFake
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClientFake
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import java.time.Clock

class LokalApplicationContext(clock: Clock = TikkendeKlokke()) : ApplicationContext(clock) {
    override val texasClient = TexasClientFake()

    override val tmsVarselClient = TmsVarselClientFake()

    override val tmsMikrofrontendClient = TmsMikrofrontendClientFake()
}
