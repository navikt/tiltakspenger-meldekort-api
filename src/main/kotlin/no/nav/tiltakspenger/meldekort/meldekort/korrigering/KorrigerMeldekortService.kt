package no.nav.tiltakspenger.meldekort.meldekort.korrigering

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.bruker.BrukerSakRepo
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.FeilVedKorrigeringAvMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.KorrigerMeldekortCommand
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDag
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortRepo
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.meldeperiode.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.varsler.SakVarselRepo
import java.time.Clock

class KorrigerMeldekortService(
    private val meldekortRepo: MeldekortRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val brukerSakRepo: BrukerSakRepo,
    private val sakVarselRepo: SakVarselRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    fun korriger(command: KorrigerMeldekortCommand): Either<FeilVedKorrigeringAvMeldekort, BrukersMeldekort> {
        val meldekortSomKorrigeres = meldekortRepo.hentForMeldekortId(command.meldekortId, command.fnr)!!

        if (!meldekortSomKorrigeres.harMinstEnEndring(command.korrigerteDager)) {
            return FeilVedKorrigeringAvMeldekort.IngenEndringer.left()
        }

        val meldekortForKjede =
            meldekortRepo.hentMeldekortForKjedeId(meldekortSomKorrigeres.meldeperiode.kjedeId, command.fnr)

        val sisteMeldeperiode = meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
            id = meldekortSomKorrigeres.meldeperiode.kjedeId,
            fnr = command.fnr,
        )!!

        return meldekortForKjede.korriger(command, sisteMeldeperiode, clock).onRight {
            sessionFactory.withTransactionContext { tx ->
                meldekortRepo.lagre(it, tx)
                sakVarselRepo.flaggForVarselvurdering(it.sakId, tx)
            }
        }
    }

    /**
     *  @return [BrukersMeldekort] for [meldekortId], siste [Meldeperiode] for meldekortets kjede, og flagg for melding i helg
     * */
    fun hentForKorrigering(meldekortId: MeldekortId, fnr: Fnr): Triple<BrukersMeldekort, Meldeperiode, Boolean> {
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, fnr)

        requireNotNull(meldekort) {
            "Fant ikke meldekort med id $meldekortId"
        }

        requireNotNull(meldekort.mottatt) {
            "Meldekortet må være mottatt for å kunne korrigeres - id: $meldekortId"
        }

        val sisteMeldeperiode =
            meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(meldekort.meldeperiode.kjedeId, fnr)!!

        val sak = brukerSakRepo.hentForBruker(sisteMeldeperiode.fnr)!!

        return Triple(meldekort, sisteMeldeperiode, sak.kanSendeInnHelgForMeldekort)
    }

    fun kanMeldekortKorrigeres(meldekortId: MeldekortId, fnr: Fnr): Boolean {
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, fnr)
        requireNotNull(meldekort) {
            "Fant ikke meldekort med id $meldekortId"
        }
        val kjede = meldekortRepo.hentMeldekortForKjedeId(meldekort.meldeperiode.kjedeId, fnr)

        return kjede.kanMeldekortKorrigeres(meldekort.id)
    }

    private fun BrukersMeldekort.harMinstEnEndring(korrigerteDager: List<MeldekortDag>): Boolean {
        return korrigerteDager.zip(this.dager).any { (korrigerDag, eksisterendeDag) ->
            korrigerDag.status != eksisterendeDag.status
        }
    }
}
