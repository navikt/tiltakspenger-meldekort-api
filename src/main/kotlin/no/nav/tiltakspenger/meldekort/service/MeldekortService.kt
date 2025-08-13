package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.validerLagring
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodeRepo
import java.time.Clock
import java.time.LocalDateTime

class MeldekortService(
    val meldekortRepo: MeldekortRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
    val sessionFactory: SessionFactory,
    val clock: Clock,
) {
    fun lagreMeldekortFraBruker(kommando: LagreMeldekortFraBrukerKommando) {
        val meldekortId = kommando.id
        val innsenderFnr = kommando.fnr
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, innsenderFnr)

        requireNotNull(meldekort) {
            "Meldekort med id $meldekortId finnes ikke for bruker ${innsenderFnr.verdi}"
        }

        require(meldekort.mottatt == null) {
            "Meldekort med id $meldekortId er allerede mottatt (${meldekort.mottatt})"
        }

        require(meldekort.deaktivert == null) {
            "Meldekort med id $meldekortId er deaktivert (${meldekort.deaktivert})"
        }

        val meldeperiode = meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
            id = meldekort.meldeperiode.kjedeId,
            fnr = innsenderFnr,
        )

        requireNotNull(meldeperiode) {
            "Meldeperiode for meldekort med id $meldekortId finnes ikke for bruker ${innsenderFnr.verdi}"
        }

        meldekort.validerLagring(meldeperiode, kommando.dager.map { it.tilMeldekortDag() }).also {
            meldekortRepo.lagreFraBruker(kommando)
        }
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

    fun korriger(command: KorrigerMeldekortCommand): Either<String, Meldekort> {
        val meldekortSomKorrigeres = meldekortRepo.hentForMeldekortId(command.meldekortId, command.fnr)!!
        val meldekortForKjede =
            meldekortRepo.hentMeldekortForKjedeId(meldekortSomKorrigeres.meldeperiode.kjedeId, command.fnr)

        val sisteMeldeperiode = meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
            id = meldekortSomKorrigeres.meldeperiode.kjedeId,
            fnr = command.fnr,
        )!!

        val korrigertMeldekort = meldekortForKjede.korriger(command, sisteMeldeperiode, clock)

        return korrigertMeldekort
            .also {
                if (it.second) {
                    meldekortRepo.opprett(it.first)
                } else {
                    meldekortRepo.oppdater(it.first)
                }
            }.first.right()
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
}
