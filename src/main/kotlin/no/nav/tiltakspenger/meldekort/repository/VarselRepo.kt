package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsler

interface VarselRepo {

    /**
     * Lagrer (insert eller update) et varsel. Domenet eier dataene.
     * @param aktiveringsmetadata Valgfri metadata fra klienten ved aktivering, kun for debugging i databasen.
     * @param inaktiveringsmetadata Valgfri metadata fra klienten ved inaktivering, kun for debugging i databasen.
     */
    fun lagre(
        varsel: Varsel,
        aktiveringsmetadata: String? = null,
        inaktiveringsmetadata: String? = null,
        sessionContext: SessionContext? = null,
    )

    /** Henter alle varsler for en gitt sak. */
    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): Varsler

    /** Henter varsler i tilstanden [Varsel.SkalAktiveres] hvor tidspunktet er passert. Brukes av aktiveringsjobben. */
    fun hentVarslerSomSkalAktiveres(
        limit: Int = 25,
        sessionContext: SessionContext? = null,
    ): List<Varsel.SkalAktiveres>

    /** Henter varsler i tilstanden [Varsel.SkalInaktiveres] hvor tidspunktet er passert. Brukes av inaktiveringsjobben. */
    fun hentVarslerSomSkalInaktiveres(
        limit: Int = 25,
        sessionContext: SessionContext? = null,
    ): List<Varsel.SkalInaktiveres>
}
