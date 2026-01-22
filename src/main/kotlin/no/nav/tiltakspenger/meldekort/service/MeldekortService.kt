package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.meldekort.domene.FeilVedKorrigeringAvMeldekort
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortForKjede
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.repository.SakRepo
import java.time.Clock
import java.time.LocalDateTime

class MeldekortService(
    val meldekortRepo: MeldekortRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
    val sakRepo: SakRepo,
    val clock: Clock,
) {
    fun lagreMeldekortFraBruker(kommando: LagreMeldekortFraBrukerKommando) {
        val meldekortId = kommando.id
        val innsenderFnr = kommando.fnr
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, innsenderFnr)!!

        require(meldekort.mottatt == null) {
            "Meldekort med id $meldekortId er allerede mottatt (${meldekort.mottatt})"
        }
        val sisteMeldeperiode = meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
            id = meldekort.meldeperiode.kjedeId,
            fnr = innsenderFnr,
        )!!
        require(meldekort.meldeperiode.id == sisteMeldeperiode.id) {
            "Meldekortets meldeperiode-id (${meldekort.meldeperiode.id}) må være den siste meldeperioden (${sisteMeldeperiode.id})"
        }

        val nyeDager = kommando.dager.map { it.tilMeldekortDag() }
        meldekortRepo.lagre(
            meldekort.fyllUtMeldekortFraBruker(
                brukerutfylteDager = nyeDager,
                clock = clock,
            ),
        )
    }

    fun hentForMeldekortId(id: MeldekortId, fnr: Fnr): Meldekort? {
        return meldekortRepo.hentForMeldekortId(id, fnr)
    }

    fun hentSisteUtfylteMeldekort(fnr: Fnr): Meldekort? {
        return meldekortRepo.hentSisteUtfylteMeldekort(fnr)
    }

    fun hentNesteMeldekortForUtfylling(fnr: Fnr): Meldekort? {
        return meldekortRepo.hentNesteMeldekortTilUtfylling(fnr)
    }

    fun hentInnsendteMeldekort(fnr: Fnr): List<Meldekort> {
        return meldekortRepo.hentInnsendteMeldekortForBruker(fnr)
    }

    fun hentMeldekortSomSkalSendesTilSaksbehandling(): List<Meldekort> {
        return meldekortRepo.hentMeldekortForSendingTilSaksbehandling()
    }

    fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
    ) {
        meldekortRepo.markerSendtTilSaksbehandling(id, sendtTidspunkt)
    }

    fun korriger(command: KorrigerMeldekortCommand): Either<FeilVedKorrigeringAvMeldekort, Meldekort> {
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
            meldekortRepo.lagre(it)
        }
    }

    /**
     *  @return [Meldekort] for [meldekortId], siste [Meldeperiode] for meldekortets kjede, og flagg for melding i helg
     * */
    fun hentForKorrigering(meldekortId: MeldekortId, fnr: Fnr): Triple<Meldekort, Meldeperiode, Boolean> {
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, fnr)

        requireNotNull(meldekort) {
            "Fant ikke meldekort med id $meldekortId"
        }

        requireNotNull(meldekort.mottatt) {
            "Meldekortet må være mottatt for å kunne korrigeres - id: $meldekortId"
        }

        /**
         *  Her kunne vi kanskje også validere at meldekortet er det siste innsendte, men da må frontend oppdateres til
         *  ikke å kalle korrigering-endepunktet etter at korrigeringen er sendt inn
         */

        val sisteMeldeperiode =
            meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(meldekort.meldeperiode.kjedeId, fnr)!!

        val sak = sakRepo.hentTilBruker(sisteMeldeperiode.fnr)!!

        return Triple(meldekort, sisteMeldeperiode, sak.kanSendeInnHelgForMeldekort)
    }

    fun hentMeldekortForKjede(kjedeId: MeldeperiodeKjedeId, fnr: Fnr): MeldekortForKjede {
        return meldekortRepo.hentMeldekortForKjedeId(kjedeId, fnr)
    }

    fun hentInformasjonOmMeldekortForMicrofrontend(fnr: Fnr, clock: Clock): Pair<Int, LocalDateTime?> {
        val meldekortKlarForInnsending = meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr)
        val antallMeldekortKlarTilInnsending = meldekortKlarForInnsending.size
        val nesteMuligeInnsending = meldekortRepo.hentNesteMeldekortTilUtfylling(fnr)?.klarTilInnsendingDateTime(clock)

        return Pair(antallMeldekortKlarTilInnsending, nesteMuligeInnsending)
    }

    fun kanMeldekortKorrigeres(meldekortId: MeldekortId, fnr: Fnr): Boolean {
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, fnr)
        requireNotNull(meldekort) {
            "Fant ikke meldekort med id $meldekortId"
        }
        val kjede = meldekortRepo.hentMeldekortForKjedeId(meldekort.meldeperiode.kjedeId, fnr)

        return kjede.kanMeldekortKorrigeres(meldekort.id)
    }

    private fun Meldekort.harMinstEnEndring(korrigerteDager: List<MeldekortDag>): Boolean {
        return korrigerteDager.zip(this.dager).any { (korrigerDag, eksisterendeDag) ->
            korrigerDag.status != eksisterendeDag.status
        }
    }
}
