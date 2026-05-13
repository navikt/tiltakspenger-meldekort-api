package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.meldeperiode.MeldeperiodeRepo
import kotlin.collections.filter
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

    /**
     * Henter siste versjon av hver meldeperiodekjede for en sak, sortert på perioden.
     * Speiler [no.nav.tiltakspenger.meldekort.meldeperiode.infra.MeldeperiodePostgresRepo.hentSisteMeldeperioderForSakId]:
     * `select distinct on (fra_og_med) ... order by fra_og_med, versjon desc`
     * dvs. siste versjon per fraOgMed, sortert stigende på fraOgMed.
     */
    fun hentForSakId(sakId: SakId): List<Meldeperiode> {
        return data.get().values
            .filter { it.sakId == sakId }
            .groupBy { it.periode.fraOgMed }
            .map { (_, meldeperioder) -> meldeperioder.maxBy { it.versjon } }
            .sortedBy { it.periode.fraOgMed }
    }
}
