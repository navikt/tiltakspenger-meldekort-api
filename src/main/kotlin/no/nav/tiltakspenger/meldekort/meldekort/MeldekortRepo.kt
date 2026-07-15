package no.nav.tiltakspenger.meldekort.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

interface MeldekortRepo {

    /**
     * Oppretter et nytt meldekort hvis meldekort.id ikke finnes, hvis ikke oppdateres det eksisterende meldekortet.
     * Domenet har ansvaret for innholdet.
     */
    fun lagre(
        meldekort: BrukersMeldekort,
        sessionContext: SessionContext? = null,
    )

    /**
     * Skriver brukerens innsending ([BrukersMeldekort.mottatt], [BrukersMeldekort.dager] og [BrukersMeldekort.locale]) til et eksisterende meldekort, men kun dersom det fortsatt er åpent for innsending (ikke allerede mottatt og ikke deaktivert).
     * Dette håndhever atomisk i databasen at kun et meldekort i [no.nav.tiltakspenger.meldekort.meldekort.MeldekortStatus.KAN_UTFYLLES] kan sendes inn, og lukker race-vinduet mellom lesing og skriving ved samtidige innsendinger.
     *
     * @return antall rader som ble oppdatert: 1 ved suksess, 0 dersom meldekortet i mellomtiden ble mottatt
     * eller deaktivert (eller ikke finnes).
     */
    fun lagreInnsendtMeldekortFraBruker(
        meldekort: BrukersMeldekort,
        sessionContext: SessionContext? = null,
    ): Int

    /**
     * Deaktiverer et ikke-mottatt/ikke-deaktivert meldekort ved å sette deaktiveringsdato til nå.
     * Deaktiverte meldekort skal ikke vises i appen, og skal ikke kunne sendes inn.
     */
    fun deaktiver(
        meldekortId: MeldekortId,
        sessionContext: SessionContext? = null,
    )

    fun hentForMeldekortId(
        meldekortId: MeldekortId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): MeldekortForKjede

    fun hentSisteUtfylteMeldekort(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentNesteMeldekortTilUtfylling(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentAlleMeldekortKlarTilInnsending(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun hentInnsendteMeldekortForBruker(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): List<MeldekortMedSisteMeldeperiode>

    fun hentSisteMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?
}
