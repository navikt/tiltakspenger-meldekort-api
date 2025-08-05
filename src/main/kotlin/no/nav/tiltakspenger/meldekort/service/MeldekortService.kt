package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.validerLagring
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.Clock
import java.time.LocalDateTime

class MeldekortService(
    val meldekortRepo: MeldekortRepo,
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

        val meldeperiode = meldekortRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
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

    fun hentAlleMeldekort(fnr: Fnr): List<Meldekort> {
        return meldekortRepo.hentAlleMeldekortForBruker(fnr)
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
        val eksisterendeMeldekort = meldekortRepo.hentForMeldekortId(command.meldekortId, command.fnr)
            ?: return "Meldekort med id $command.meldekortId finnes ikke for bruker ${command.fnr.verdi}".left()

        require(eksisterendeMeldekort.status == MeldekortStatus.INNSENDT) {
            "Meldekort med id ${eksisterendeMeldekort.id} er ikke i status INNSENDT, kan ikke korrigeres. Nåværende status: ${eksisterendeMeldekort.status}"
        }

        val meldeperiode = meldekortRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
            id = eksisterendeMeldekort.meldeperiode.kjedeId,
            fnr = command.fnr,
        )

        requireNotNull(meldeperiode) {
            "Meldeperiode for meldekort med id ${eksisterendeMeldekort.id} finnes ikke for bruker ${command.fnr.verdi}"
        }

        val sisteMeldekortForKjeden = meldekortRepo.hentSisteMeldekortForKjedeId(
            kjedeId = eksisterendeMeldekort.meldeperiode.kjedeId,
            fnr = command.fnr,
        ) ?: return "Siste meldekort for kjede ${eksisterendeMeldekort.meldeperiode.kjedeId} finnes ikke for bruker ${command.fnr.verdi}".left()

        // TODO - Hva er tilbakemeldingen vi vil gi bruker? Prøv igjen senere?
        require(eksisterendeMeldekort == sisteMeldekortForKjeden) {
            "Meldekort med id ${eksisterendeMeldekort.id} er ikke siste meldekort i kjeden ${eksisterendeMeldekort.meldeperiode.kjedeId}. Kan ikke korrigere."
        }

        val nyttMeldekortFraBruker = Meldekort(
            id = MeldekortId.random(),
            deaktivert = null,
            mottatt = LocalDateTime.now(clock),
            meldeperiode = meldeperiode,
            dager = command.korrigerteDager,
            journalpostId = null,
            journalføringstidspunkt = null,
            varselId = null,
            erVarselInaktivert = false,
        )

        // vil trigge en 'ukjent feil' for bruker ved feil som vil være litt dårlig ux
        nyttMeldekortFraBruker.validerLagring(meldeperiode, command.korrigerteDager).also {
            sessionFactory.withTransactionContext { tx ->
                meldekortRepo.opprett(nyttMeldekortFraBruker)
            }
        }

        return nyttMeldekortFraBruker.right()
    }
}

data class KorrigerMeldekortCommand(
    val meldekortId: MeldekortId,
    val fnr: Fnr,
    val korrigerteDager: List<MeldekortDag>,
)
