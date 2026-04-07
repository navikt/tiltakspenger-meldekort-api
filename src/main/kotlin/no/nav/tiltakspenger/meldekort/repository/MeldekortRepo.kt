package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.clients.varsler.SendtVarselMetadata
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortForKjede
import no.nav.tiltakspenger.meldekort.domene.MeldekortMedSisteMeldeperiode
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import java.time.LocalDateTime

interface MeldekortRepo {

    /**
     * Oppretter et nytt meldekort hvis meldekort.id ikke finnes, hvis ikke oppdateres det eksisterende meldekortet.
     * Domenet har ansvaret for innholdet.
     */
    fun lagre(
        meldekort: Meldekort,
        sessionContext: SessionContext? = null,
    )

    /**
     * Deaktiverer et ikke-mottatt/ikke-deaktivert meldekort ved å sette deaktiveringsdato til nå.
     * Deaktiverte meldekort skal ikke vises i appen, og skal ikke kunne sendes inn.
     * @param deaktiverVarsel dersom true vil også tilhørende varsel bli inaktivert (ved hjelp av en jobb).
     */
    fun deaktiver(
        meldekortId: MeldekortId,
        deaktiverVarsel: Boolean,
        sessionContext: SessionContext? = null,
    )

    fun hentForMeldekortId(
        meldekortId: MeldekortId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldekort?

    fun hentMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): MeldekortForKjede

    fun hentSisteUtfylteMeldekort(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldekort?

    fun hentNesteMeldekortTilUtfylling(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldekort?

    fun hentAlleMeldekortKlarTilInnsending(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): List<Meldekort>

    fun hentInnsendteMeldekortForBruker(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): List<MeldekortMedSisteMeldeperiode>

    fun hentMeldekortForSendingTilSaksbehandling(sessionContext: SessionContext? = null): List<Meldekort>

    fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
        sessionContext: SessionContext? = null,
    )

    fun markerJournalført(
        meldekortId: MeldekortId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext? = null,
    )

    fun hentDeSomSkalJournalføres(limit: Int = 10, sessionContext: SessionContext? = null): List<Meldekort>

    fun hentMeldekortDetSkalVarslesFor(limit: Int = 25, sessionContext: SessionContext? = null): List<Meldekort>

    fun markerVarslet(
        meldekortId: MeldekortId,
        sendtVarselTidspunkt: LocalDateTime,
        sendtVarselMetadata: SendtVarselMetadata,
        sessionContext: SessionContext? = null,
    )

    fun henteMeldekortSomSkalInaktivereVarsel(
        limit: Int = 25,
        sessionContext: SessionContext? = null,
    ): List<Meldekort>

    fun hentSisteMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Meldekort?
}
