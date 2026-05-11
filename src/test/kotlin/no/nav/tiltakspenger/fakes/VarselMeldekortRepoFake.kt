package no.nav.tiltakspenger.fakes

import no.nav.tiltakspenger.fakes.repos.MeldekortRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldeperiodeRepoFake
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.varsler.KjedeSomManglerInnsending
import no.nav.tiltakspenger.meldekort.varsler.VarselMeldekortRepo

class VarselMeldekortRepoFake(
    private val meldekortRepoFake: MeldekortRepoFake,
    private val meldeperiodeRepoFake: MeldeperiodeRepoFake,
) : VarselMeldekortRepo {

    override fun hentFørsteKjedeSomManglerInnsending(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): KjedeSomManglerInnsending? {
        val meldeperioder = meldeperiodeRepoFake.hentAllForSakId(sakId)
        val meldekort = meldekortRepoFake.hentAlleForSakId(sakId)

        return meldeperioder
            .groupBy { it.kjedeId }
            .mapNotNull { (kjedeId, perioder) ->
                val nyeste = perioder.maxByOrNull { it.versjon } ?: return@mapNotNull null
                val harRett = nyeste.maksAntallDagerForPeriode > 0
                if (!harRett) return@mapNotNull null
                val harMottattMeldekortIKjeden = meldekort.any {
                    it.meldeperiode.kjedeId == kjedeId && it.mottatt != null
                }
                if (harMottattMeldekortIKjeden) return@mapNotNull null
                KjedeSomManglerInnsending(
                    sakId = sakId,
                    meldeperiodeId = nyeste.id,
                    kjedeId = kjedeId,
                    kanFyllesUtFraOgMed = nyeste.kanFyllesUtFraOgMed,
                )
            }.minByOrNull { it.kanFyllesUtFraOgMed }
    }
}
