package no.nav.tiltakspenger.meldekort.meldekort

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.bruker.BrukerSakRepo
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldekortvedtakRepo
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.meldeperiode.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.varsler.SakVarselRepo
import java.time.Clock
import java.time.LocalDateTime

class MeldekortService(
    val meldekortRepo: MeldekortRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
    val brukerSakRepo: BrukerSakRepo,
    val sakVarselRepo: SakVarselRepo,
    val meldekortvedtakRepo: MeldekortvedtakRepo,
    val sessionFactory: SessionFactory,
    val clock: Clock,
) {
    fun lagreMeldekortFraBruker(kommando: LagreMeldekortFraBrukerKommando) {
        val meldekortId = kommando.id
        val innsenderFnr = kommando.fnr
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, innsenderFnr)!!

        require(meldekort.mottatt == null) {
            "Meldekort med id $meldekortId er allerede mottatt (${meldekort.mottatt})"
        }

        val harMeldekortvedtakForKjede = meldekortvedtakRepo.hentForSakId(meldekort.sakId)
            .any { vedtak ->
                vedtak.meldeperiodebehandlinger.any { it.meldeperiodeKjedeId == meldekort.meldeperiode.kjedeId }
            }
        require(!harMeldekortvedtakForKjede) {
            "Meldekort med id $meldekortId kan ikke sendes inn fordi kjeden ${meldekort.meldeperiode.kjedeId} allerede er håndtert av et meldekortvedtak. En eventuell endring må gjøres som korrigering."
        }

        val sisteMeldeperiode = meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
            id = meldekort.meldeperiode.kjedeId,
            fnr = innsenderFnr,
        )!!
        require(meldekort.meldeperiode.id == sisteMeldeperiode.id) {
            "Meldekortets meldeperiode-id (${meldekort.meldeperiode.id}) må være den siste meldeperioden (${sisteMeldeperiode.id})"
        }

        val utfyltMeldekort = meldekort.fyllUtMeldekortFraBruker(
            brukerutfylteDager = kommando.dager,
            locale = kommando.locale,
            clock = clock,
        )
        sessionFactory.withTransactionContext { tx ->
            meldekortRepo.lagre(utfyltMeldekort, tx)

            // Flagg saken for varselvurdering - den asynkrone jobben håndterer selve varselet
            sakVarselRepo.flaggForVarselvurdering(utfyltMeldekort.sakId, tx)
        }
    }

    fun hentForMeldekortId(id: MeldekortId, fnr: Fnr): BrukersMeldekort? {
        return meldekortRepo.hentForMeldekortId(id, fnr)
    }

    fun hentSisteUtfylteMeldekort(fnr: Fnr): BrukersMeldekort? {
        return meldekortRepo.hentSisteUtfylteMeldekort(fnr)
    }

    fun hentNesteMeldekortForUtfylling(fnr: Fnr): BrukersMeldekort? {
        return meldekortRepo.hentNesteMeldekortTilUtfylling(fnr)
    }

    /**
     * Henter brukers innsendte meldekort, supplert med papirmeldekort fra vedtak for kjeder uten
     * digital innsending. Digitale innsendinger beholdes uendret (én rad per innsending, samme
     * rekkefølge som før). For hver kjede som kun er håndtert via meldekortvedtak (ingen digital
     * innsending — `brukersMeldekortId == null`) legges det til én rad som viser vedtakets tilstand,
     * med kjedens åpne (uinnsendte) meldekort som korrigeringsmål.
     */
    fun hentInnsendteMeldekort(fnr: Fnr): List<RegistrertMeldekortMedSisteMeldeperiode> {
        val alle = meldekortRepo.hentAlleMeldekortMedSisteMeldeperiodeForBruker(fnr)
        if (alle.isEmpty()) return emptyList()

        // Digitale innsendinger beholdes som historikk (én rad per innsending), i samme rekkefølge som før.
        val digitaleEntries = alle
            .filter { it.meldekort.erInnsendt }
            .map {
                RegistrertMeldekortMedSisteMeldeperiode(
                    registrertMeldekort = it.meldekort,
                    sisteMeldeperiode = it.sisteMeldeperiode,
                )
            }

        val meldekortvedtak = alle.map { it.meldekort.sakId }.toSet()
            .flatMap { meldekortvedtakRepo.hentForSakId(it) }

        // Kun behandlinger uten brukersMeldekortId er papir-only (ingen digital innsending vi allerede viser).
        // Et vedtak kan vurdere flere meldeperioder, så vi henter hele vedtaket og etterfiltrerer per kjede.
        val papirEntries = meldekortvedtak
            .flatMap { vedtak -> vedtak.meldeperiodebehandlinger.map { vedtak to it } }
            .filter { (_, behandling) -> behandling.brukersMeldekortId == null }
            .groupBy { (_, behandling) -> behandling.meldeperiodeKjedeId }
            .mapNotNull { (kjedeId, behandlingerForKjede) ->
                val (vedtak, behandling) = behandlingerForKjede.maxByOrNull { (v, _) -> v.opprettet }!!

                // Korrigeringsmål for en papir-only kjede er kjedens åpne (uinnsendte) meldekort.
                val åpentMeldekort = alle.firstOrNull {
                    it.meldekort.meldeperiode.kjedeId == kjedeId && !it.meldekort.erInnsendt
                } ?: return@mapNotNull null

                val vedtattMeldekort = VedtattMeldekort(
                    id = åpentMeldekort.meldekort.id,
                    meldeperiode = åpentMeldekort.meldekort.meldeperiode,
                    dager = behandling.dager.map { MeldekortDag(dag = it.dato, status = it.status) },
                    opprettet = vedtak.opprettet,
                )

                RegistrertMeldekortMedSisteMeldeperiode(
                    registrertMeldekort = vedtattMeldekort,
                    sisteMeldeperiode = åpentMeldekort.sisteMeldeperiode,
                )
            }

        return digitaleEntries + papirEntries
    }

    fun hentMeldekortSomSkalSendesTilSaksbehandling(): List<BrukersMeldekort> {
        return meldekortRepo.hentMeldekortForSendingTilSaksbehandling()
    }

    fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
    ) {
        meldekortRepo.markerSendtTilSaksbehandling(id, sendtTidspunkt)
    }

    fun korriger(command: KorrigerMeldekortCommand): Either<FeilVedKorrigeringAvMeldekort, BrukersMeldekort> {
        val meldekortSomKorrigeres = meldekortRepo.hentForMeldekortId(command.meldekortId, command.fnr)!!

        val meldekortForKjede =
            meldekortRepo.hentMeldekortForKjedeId(meldekortSomKorrigeres.meldeperiode.kjedeId, command.fnr)

        val registrertForKjede = RegistrertMeldekortForKjede(
            meldekortForKjede = meldekortForKjede,
            meldekortvedtak = meldekortvedtakRepo.hentForSakId(meldekortSomKorrigeres.sakId),
        )

        val registrert = registrertForKjede.sisteRegistrerte()
        if (registrert != null && !harMinstEnEndring(command.korrigerteDager, registrert.dager)) {
            return FeilVedKorrigeringAvMeldekort.IngenEndringer.left()
        }

        val sisteMeldeperiode = meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
            id = meldekortSomKorrigeres.meldeperiode.kjedeId,
            fnr = command.fnr,
        )!!

        return registrertForKjede.korriger(command, sisteMeldeperiode, clock).onRight { resultat ->
            sessionFactory.withTransactionContext { tx ->
                resultat.placeholderSomDeaktiveres?.let { meldekortRepo.deaktiver(it, tx) }
                meldekortRepo.lagre(resultat.korrigertMeldekort, tx)
                sakVarselRepo.flaggForVarselvurdering(resultat.korrigertMeldekort.sakId, tx)
            }
        }.map { it.korrigertMeldekort }
    }

    /**
     *  @return kjedens siste registrerte tilstand ([RegistrertMeldekort]) — digital eller meldekortvedtak,
     *  siste [Meldeperiode] for meldekortets kjede, og flagg for melding i helg
     * */
    fun hentForKorrigering(meldekortId: MeldekortId, fnr: Fnr): Triple<RegistrertMeldekort, Meldeperiode, Boolean> {
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, fnr)

        requireNotNull(meldekort) {
            "Fant ikke meldekort med id $meldekortId"
        }

        val registrertForKjede = RegistrertMeldekortForKjede(
            meldekortForKjede = meldekortRepo.hentMeldekortForKjedeId(meldekort.meldeperiode.kjedeId, fnr),
            meldekortvedtak = meldekortvedtakRepo.hentForSakId(meldekort.sakId),
        )

        val registrert = registrertForKjede.sisteRegistrerte()
        requireNotNull(registrert) {
            "Fant ingen registrert tilstand å korrigere for meldekort $meldekortId"
        }
        require(registrert.id == meldekortId) {
            "Meldekortet $meldekortId er ikke siste registrerte tilstand i kjeden og kan ikke korrigeres"
        }

        val sisteMeldeperiode =
            meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(meldekort.meldeperiode.kjedeId, fnr)!!

        val sak = brukerSakRepo.hentForBruker(sisteMeldeperiode.fnr)!!

        return Triple(registrert, sisteMeldeperiode, sak.kanSendeInnHelgForMeldekort)
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
        val registrertForKjede = RegistrertMeldekortForKjede(
            meldekortForKjede = meldekortRepo.hentMeldekortForKjedeId(meldekort.meldeperiode.kjedeId, fnr),
            meldekortvedtak = meldekortvedtakRepo.hentForSakId(meldekort.sakId),
        )

        return registrertForKjede.kanKorrigeres(meldekort.id)
    }

    private fun harMinstEnEndring(
        korrigerteDager: List<MeldekortDag>,
        eksisterendeDager: List<MeldekortDag>,
    ): Boolean {
        return korrigerteDager.zip(eksisterendeDager).any { (korrigerDag, eksisterendeDag) ->
            korrigerDag.status != eksisterendeDag.status
        }
    }
}
