package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode

interface MeldeperiodeRepo {

    fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext? = null,
    )

    fun hentForId(id: String, sessionContext: SessionContext? = null): Meldeperiode?
    fun hentForKjedeId(kjedeId: String, sessionContext: SessionContext? = null): Meldeperiode?
}
