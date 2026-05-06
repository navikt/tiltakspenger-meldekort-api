package no.nav.tiltakspenger.fakes

import no.nav.tiltakspenger.fakes.repos.MeldekortRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldeperiodeRepoFake
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.varsler.BeskjedMeldeperiode
import no.nav.tiltakspenger.meldekort.domene.varsler.GirRettEndring
import no.nav.tiltakspenger.meldekort.domene.varsler.KjedeSomManglerInnsending
import no.nav.tiltakspenger.meldekort.domene.varsler.MeldeperiodeEndring
import no.nav.tiltakspenger.meldekort.domene.varsler.Verdiendring
import no.nav.tiltakspenger.meldekort.repository.VarselMeldekortRepo

class VarselMeldekortRepoFake(
    private val meldekortRepoFake: MeldekortRepoFake,
    private val meldeperiodeRepoFake: MeldeperiodeRepoFake,
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
                // Oppgave-sporet gjelder kun kjeder hvor bruker ikke har sendt inn noen versjon.
                val harInnsendt = meldekort.any {
                    it.meldeperiode.kjedeId == nyeste.kjedeId && it.mottatt != null && it.deaktivert == null
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

    override fun hentMeldeperioderSomSkalHaBeskjed(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): List<BeskjedMeldeperiode> {
        val meldeperioder = meldeperiodeRepoFake.hentAllForSakId(sakId)
        val meldekort = meldekortRepoFake.hentAlleForSakId(sakId)

        return meldeperioder
            .groupBy { it.kjedeId }
            .mapNotNull { (_, perioder) ->
                val nyeste = perioder.maxByOrNull { it.versjon } ?: return@mapNotNull null
                val sisteInnsendte = meldekort
                    .filter { it.meldeperiode.kjedeId == nyeste.kjedeId && it.mottatt != null && it.deaktivert == null }
                    .map { it.meldeperiode }
                    .maxByOrNull { it.versjon }
                    ?: return@mapNotNull null
                if (nyeste.versjon <= sisteInnsendte.versjon) return@mapNotNull null
                val endring = nyeste.diffFra(sisteInnsendte) ?: return@mapNotNull null
                BeskjedMeldeperiode(
                    sakId = sakId,
                    meldeperiodeId = nyeste.id,
                    kjedeId = nyeste.kjedeId,
                    versjon = nyeste.versjon,
                    sisteInnsendteVersjon = sisteInnsendte.versjon,
                    endring = endring,
                )
            }
            .sortedBy { it.versjon }
    }

    private fun Meldeperiode.diffFra(forrige: Meldeperiode): MeldeperiodeEndring? {
        val maksAntallDagerEndring = if (maksAntallDagerForPeriode == forrige.maksAntallDagerForPeriode) {
            null
        } else {
            Verdiendring(fra = forrige.maksAntallDagerForPeriode, til = maksAntallDagerForPeriode)
        }
        val girRettEndringer = girRett.entries.mapNotNull { (dato, nyVerdi) ->
            val forrigeVerdi = forrige.girRett.getValue(dato)
            if (forrigeVerdi == nyVerdi) null else GirRettEndring(dato = dato, fra = forrigeVerdi, til = nyVerdi)
        }
        return if (maksAntallDagerEndring == null && girRettEndringer.isEmpty()) {
            null
        } else {
            MeldeperiodeEndring(maksAntallDagerForPeriode = maksAntallDagerEndring, girRett = girRettEndringer)
        }
    }
}
