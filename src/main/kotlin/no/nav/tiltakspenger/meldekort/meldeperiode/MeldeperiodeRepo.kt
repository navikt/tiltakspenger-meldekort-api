package no.nav.tiltakspenger.meldekort.meldeperiode

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

interface MeldeperiodeRepo {

    fun hentForId(id: MeldeperiodeId, sessionContext: SessionContext? = null): Meldeperiode?

    fun hentSisteMeldeperiodeForMeldeperiodeKjedeId(
        id: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldeperiode?

    fun hentMeldeperiodeForPeriode(
        periode: Periode,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldeperiode?
}
