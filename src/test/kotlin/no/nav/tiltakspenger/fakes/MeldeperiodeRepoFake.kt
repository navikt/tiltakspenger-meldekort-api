package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo

class MeldeperiodeRepoFake : MeldeperiodeRepo {
    private val data = Atomic(mutableMapOf<MeldeperiodeId, Meldeperiode>())

    override fun lagre(meldeperiode: Meldeperiode, sessionContext: SessionContext?) {
        data.get()[meldeperiode.id] = meldeperiode
    }

    override fun hentForId(id: MeldeperiodeId, sessionContext: SessionContext?): Meldeperiode? {
        return data.get()[id]
    }

    override fun hentForKjedeId(kjedeId: MeldeperiodeKjedeId, sessionContext: SessionContext?): Meldeperiode? {
        return data.get().values.find { it.kjedeId == kjedeId }
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, sessionContext: SessionContext?) {
        val meldeperiode = data.get().values.find { it.fnr == gammeltFnr }
        meldeperiode?.let {
            data.get()[it.id] = it.copy(
                fnr = nyttFnr,
            )
        }
    }
}
