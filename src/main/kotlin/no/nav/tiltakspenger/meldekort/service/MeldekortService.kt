package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortForKjede
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class MeldekortService(
    val meldekortRepo: MeldekortRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
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
                mottatt = kommando.mottatt,
                brukerutfylteDager = nyeDager,
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

    fun korriger(command: KorrigerMeldekortCommand): Meldekort {
        val meldekortSomKorrigeres = meldekortRepo.hentForMeldekortId(command.meldekortId, command.fnr)!!
        val meldekortForKjede =
            meldekortRepo.hentMeldekortForKjedeId(meldekortSomKorrigeres.meldeperiode.kjedeId, command.fnr)

        val sisteMeldeperiode = meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
            id = meldekortSomKorrigeres.meldeperiode.kjedeId,
            fnr = command.fnr,
        )!!

        return meldekortForKjede.korriger(command, sisteMeldeperiode, clock).also {
            meldekortRepo.lagre(it)
        }
    }

    fun hentMeldeperiodeForPeriode(periode: Periode, fnr: Fnr): PreutfyltKorrigering {
        val meldeperiode = meldeperiodeRepo.hentMeldeperiodeForPeriode(periode, fnr)!!

        val meldekort: Meldekort =
            meldekortRepo.hentMeldekortForKjedeId(meldeperiode.kjedeId, fnr).sisteInnsendteMeldekort()!!

        val dager = meldeperiode.girRett.toList().zip(meldekort.dager).map { (girRett, meldekortDag) ->
            MeldekortDag(
                dag = meldekortDag.dag,
                status = if (girRett.second) {
                    if (meldekortDag.status == MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                        MeldekortDagStatus.IKKE_BESVART
                    } else {
                        meldekortDag.status
                    }
                } else {
                    MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                },
            )
        }

        return PreutfyltKorrigering(
            meldeperiodeId = meldeperiode.id,
            kjedeId = meldeperiode.kjedeId,
            dager = dager,
            periode = meldeperiode.periode,
            mottattTidspunktSisteMeldekort = meldekort.mottatt!!,
        )
    }

    fun hentMeldekortForKjede(kjedeId: MeldeperiodeKjedeId, fnr: Fnr): MeldekortForKjede {
        return meldekortRepo.hentMeldekortForKjedeId(kjedeId, fnr)
    }

    fun hentInformasjonOmMeldekortForMicrofrontend(fnr: Fnr): Pair<Int, LocalDate?> {
        val meldekortKlarForInnsending = meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr)
        val antallMeldekortKlarTilInnsending = meldekortKlarForInnsending.size
        val nesteMuligeInnsending = meldekortRepo.hentNesteMeldekortTilUtfylling(fnr)?.klarTilInnsendingDag

        return Pair(antallMeldekortKlarTilInnsending, nesteMuligeInnsending)
    }
}
