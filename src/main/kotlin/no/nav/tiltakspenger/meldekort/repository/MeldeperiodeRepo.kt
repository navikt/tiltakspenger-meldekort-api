package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode

interface MeldeperiodeRepo {

    fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext? = null,
    )

    fun hentForId(id: HendelseId, sessionContext: SessionContext? = null): Meldeperiode?
    fun hentForKjedeId(kjedeId: MeldeperiodeKjedeId, sessionContext: SessionContext? = null): Meldeperiode?
}
