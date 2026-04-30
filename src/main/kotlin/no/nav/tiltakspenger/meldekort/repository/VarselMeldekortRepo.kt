package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.varsler.KjedeSomManglerInnsending

/**
 * Repo for å hente data fra meldeperiode/meldekort_bruker i kontekst av varselvurdering.
 * Holdes separat fra bruker-repos for å ikke forurense brukerdomenet.
 */
interface VarselMeldekortRepo {

    /**
     * Finner alle meldeperiodekjeder for en gitt sak hvor nyeste meldeperiode-versjon
     * ikke har et tilhørende innsendt (mottatt), ikke-deaktivert meldekort.
     *
     * En kjede "mangler innsending" dersom:
     * 1. Den nyeste meldeperiode-versjonen i kjeden har minst én dag som gir rett
     * 2. Det ikke finnes et meldekort knyttet til denne nyeste versjonen som er mottatt og ikke deaktivert
     */
    fun hentKjederSomManglerInnsending(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): List<KjedeSomManglerInnsending>
}
