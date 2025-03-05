package no.nav.tiltakspenger

import no.nav.tiltakspenger.fakes.MeldekortRepoFake
import no.nav.tiltakspenger.fakes.MeldeperiodeRepoFake
import no.nav.tiltakspenger.fakes.TexasFake
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClientFake
import no.nav.tiltakspenger.meldekort.context.ApplicationContext

/**
 * Oppretter en tom ApplicationContext for bruk i tester.
 * Dette vil tilsvare en tom intern database og tomme fakes for eksterne tjenester.
 * Bruk service-funksjoner og hjelpemetoder for å legge til data.
 */
class TestApplicationContext : ApplicationContext() {
    override val texasHttpClient = TexasFake()

    override val tmsVarselClient = TmsVarselClientFake()

    override val meldekortRepo = MeldekortRepoFake()

    override val meldeperiodeRepo = MeldeperiodeRepoFake()
}
