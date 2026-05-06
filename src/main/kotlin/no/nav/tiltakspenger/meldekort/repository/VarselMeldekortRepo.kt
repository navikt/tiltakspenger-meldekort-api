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
     * Finner første meldeperiodekjede for en gitt sak hvor vi aldri har mottatt et meldekort,
     * sortert på når kjeden tidligst kan fylles ut.
     *
     * En kjede "mangler innsending" dersom:
     * 1. Den nyeste meldeperiode-versjonen i kjeden har minst én dag som gir rett
     * 2. Det ikke finnes et mottatt meldekort for noen meldeperiode i kjeden
     */
    fun hentFørsteKjedeSomManglerInnsending(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): KjedeSomManglerInnsending?
}
