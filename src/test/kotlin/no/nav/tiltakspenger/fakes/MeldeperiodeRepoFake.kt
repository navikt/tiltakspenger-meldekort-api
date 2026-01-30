package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.lastOrNull
import kotlin.collections.maxByOrNull
import kotlin.collections.mutableMapOf
import kotlin.collections.set

class MeldeperiodeRepoFake : MeldeperiodeRepo {
    private val data = Atomic(mutableMapOf<MeldeperiodeId, Meldeperiode>())

    override fun lagre(meldeperiode: Meldeperiode, sessionContext: SessionContext?) {
        data.get()[meldeperiode.id] = meldeperiode
    }

    override fun hentForId(id: MeldeperiodeId, sessionContext: SessionContext?): Meldeperiode? {
        return data.get()[id]
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, sessionContext: SessionContext?) {
        val meldeperiode = data.get().values.find { it.fnr == gammeltFnr }
        meldeperiode?.let {
            data.get()[it.id] = it.copy(
                fnr = nyttFnr,
            )
        }
    }

    override fun hentSisteMeldeperiodeForMeldeperiodeKjedeId(
        id: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Meldeperiode? {
        return data.get()
            .filter { it.value.kjedeId == id && it.value.fnr == fnr }
            .values
            .maxByOrNull { it.versjon }
    }

    override fun hentMeldeperiodeForPeriode(
        periode: Periode,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Meldeperiode? {
        return data.get().filter { it.value.periode == periode }.values.lastOrNull()
    }
}
