package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode

interface MeldeperiodeRepo {

    fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext? = null,
    )

    fun hentForId(id: MeldeperiodeId, sessionContext: SessionContext? = null): Meldeperiode?

    fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, sessionContext: SessionContext? = null)

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
