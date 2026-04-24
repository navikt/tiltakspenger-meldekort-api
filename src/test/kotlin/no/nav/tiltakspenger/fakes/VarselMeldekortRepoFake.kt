package no.nav.tiltakspenger.fakes

import no.nav.tiltakspenger.fakes.repos.MeldekortRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldeperiodeRepoFake
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.varsler.KjedeSomManglerInnsending
import no.nav.tiltakspenger.meldekort.repository.VarselMeldekortRepo
import java.time.Clock
import java.time.LocalDateTime

class VarselMeldekortRepoFake(
    private val meldekortRepoFake: MeldekortRepoFake,
    private val meldeperiodeRepoFake: MeldeperiodeRepoFake,
    private val clock: Clock,
) : VarselMeldekortRepo {

    override fun hentKjederSomManglerInnsending(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): List<KjedeSomManglerInnsending> {
        val meldeperioder = meldeperiodeRepoFake.hentAllForSakId(sakId)
        val meldekort = meldekortRepoFake.hentAlleForSakId(sakId)

        // Grupper meldeperioder per kjede, finn nyeste versjon
        return meldeperioder
            .groupBy { it.kjedeId }
            .mapNotNull { (kjedeId, perioder) ->
                val nyeste = perioder.maxByOrNull { it.versjon } ?: return@mapNotNull null
                // Sjekk om nyeste versjon har minst én dag med rett
                val harRett = nyeste.maksAntallDagerForPeriode > 0
                if (!harRett) return@mapNotNull null
                val kanFyllesUtNå = nyeste.kanFyllesUtFraOgMed <= LocalDateTime.now(clock)
                if (!kanFyllesUtNå) return@mapNotNull null
                // Sjekk om det finnes et innsendt, ikke-deaktivert meldekort for denne nyeste versjonen
                val harInnsendt = meldekort.any {
                    it.meldeperiode.id == nyeste.id && it.mottatt != null && it.deaktivert == null
                }
                if (harInnsendt) return@mapNotNull null
                KjedeSomManglerInnsending(
                    sakId = sakId,
                    meldeperiodeId = nyeste.id,
                    kjedeId = kjedeId,
                    nyesteVersjon = nyeste.versjon,
                    kanFyllesUtFraOgMed = nyeste.kanFyllesUtFraOgMed,
                )
            }
            .sortedBy { it.kanFyllesUtFraOgMed }
    }
}
