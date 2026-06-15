package no.nav.tiltakspenger.meldekort.meldekort

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.meldeperiode.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.varsler.SakVarselRepo
import java.time.Clock

class LagreMeldekortFraBrukerService(
    private val meldekortRepo: MeldekortRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val sakVarselRepo: SakVarselRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    fun lagreMeldekortFraBruker(
        kommando: LagreMeldekortFraBrukerKommando,
    ): Either<KunneIkkeLagreMeldekortFraBruker, Unit> {
        return Either.catch {
            lagreMeldekortFraBrukerInternal(kommando)
        }.getOrElse {
            // Uventede exceptions (domeneinvarianter, DB-feil o.l.) legges inn i eithern slik at kalleren
            // får én samlet feiltype å forholde seg til. throwable kan være sensitiv og logges i sikkerlogg.
            KunneIkkeLagreMeldekortFraBruker.UventetFeilVedLagring(kommando.id, it).left()
        }
    }

    private fun lagreMeldekortFraBrukerInternal(
        kommando: LagreMeldekortFraBrukerKommando,
    ): Either<KunneIkkeLagreMeldekortFraBruker, Unit> {
        val meldekortId = kommando.id
        val innsenderFnr = kommando.fnr

        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, innsenderFnr)
            ?: return KunneIkkeLagreMeldekortFraBruker.FantIkkeMeldekort(meldekortId).left()

        if (meldekort.erInnsendt) {
            return KunneIkkeLagreMeldekortFraBruker.MeldekortErAlleredeMottatt(meldekortId).left()
        }

        val sisteMeldeperiode = meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
            id = meldekort.meldeperiode.kjedeId,
            fnr = innsenderFnr,
        ) ?: return KunneIkkeLagreMeldekortFraBruker.FantIkkeMeldeperiode(
            meldekortId = meldekortId,
            kjedeId = meldekort.meldeperiode.kjedeId,
        ).left()

        if (meldekort.meldeperiode.id != sisteMeldeperiode.id) {
            return KunneIkkeLagreMeldekortFraBruker.MeldekortetsMeldeperiodeErErstattet(
                meldekortId = meldekortId,
                meldekortetsMeldeperiodeId = meldekort.meldeperiode.id,
                sisteMeldeperiodeId = sisteMeldeperiode.id,
            ).left()
        }

        return when (val status = meldekort.status(clock)) {
            MeldekortStatus.KAN_UTFYLLES -> {
                val utfyltMeldekort = meldekort.fyllUtMeldekortFraBruker(
                    brukerutfylteDager = kommando.dager,
                    locale = kommando.locale,
                    clock = clock,
                )
                sessionFactory.withTransactionContext { tx ->
                    // Betinget skriv: håndhever atomisk at meldekortet fortsatt er KAN_UTFYLLES, slik at to
                    // samtidige innsendinger ikke begge kan lagre (TOCTOU mot erInnsendt-sjekken over).
                    val antallOppdaterteRader = meldekortRepo.lagreInnsendtMeldekortFraBruker(utfyltMeldekort, tx)
                    if (antallOppdaterteRader > 0) {
                        // Flagg saken for varselvurdering - den asynkrone jobben håndterer selve varselet
                        sakVarselRepo.flaggForVarselvurdering(utfyltMeldekort.sakId, tx)
                        Unit.right()
                    } else {
                        // En samtidig innsending kom oss i forkjøpet mellom lesingen over og denne skrivingen.
                        // Les meldekortet på nytt i samme transaksjon for å avgjøre den faktiske årsaken
                        // (mottatt vs. deaktivert) og gi bruker riktig feilmelding.
                        bestemÅrsakForAvvistInnsending(meldekortId, innsenderFnr, tx).left()
                    }
                }
            }

            MeldekortStatus.IKKE_KLAR -> KunneIkkeLagreMeldekortFraBruker.MeldekortErIkkeKlartTilInnsending(
                meldekortId = meldekortId,
                status = status,
                kanPrøveIgjenTidspunkt = meldekort.meldeperiode.kanFyllesUtFraOgMed,
            ).left()

            MeldekortStatus.DEAKTIVERT ->
                KunneIkkeLagreMeldekortFraBruker.MeldekortErDeaktivert(meldekortId).left()

            // Allerede filtrert ut av erInnsendt-sjekken over; håndteres her kun for at when-et skal være
            // uttømmende.
            MeldekortStatus.INNSENDT ->
                KunneIkkeLagreMeldekortFraBruker.MeldekortErAlleredeMottatt(meldekortId).left()
        }
    }

    /**
     * Den betingede skrivingen traff 0 rader fordi meldekortet ikke lenger var åpent for innsending. Leser
     * meldekortet på nytt (i samme transaksjon som skrivingen) for å skille mellom de mulige årsakene og gi
     * bruker en presis feilmelding.
     */
    private fun bestemÅrsakForAvvistInnsending(
        meldekortId: MeldekortId,
        innsenderFnr: Fnr,
        sessionContext: SessionContext,
    ): KunneIkkeLagreMeldekortFraBruker {
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, innsenderFnr, sessionContext)
            ?: return KunneIkkeLagreMeldekortFraBruker.FantIkkeMeldekort(meldekortId)

        return when (meldekort.status(clock)) {
            MeldekortStatus.DEAKTIVERT ->
                KunneIkkeLagreMeldekortFraBruker.MeldekortErDeaktivert(meldekortId)

            MeldekortStatus.INNSENDT ->
                KunneIkkeLagreMeldekortFraBruker.MeldekortErAlleredeMottatt(meldekortId)

            // KAN_UTFYLLES/IKKE_KLAR her er en selvmotsigelse: skrivingen traff 0 rader, men meldekortet
            // framstår fortsatt som åpent. Behandles som en uventet feil slik at den havner i sikkerlogg.
            MeldekortStatus.KAN_UTFYLLES, MeldekortStatus.IKKE_KLAR ->
                KunneIkkeLagreMeldekortFraBruker.UventetFeilVedLagring(
                    meldekortId = meldekortId,
                    throwable = IllegalStateException(
                        "Betinget innsending traff 0 rader, men meldekortet framstår fortsatt som åpent for innsending",
                    ),
                )
        }
    }
}
