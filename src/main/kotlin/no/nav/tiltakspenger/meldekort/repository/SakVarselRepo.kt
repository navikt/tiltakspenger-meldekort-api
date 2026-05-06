package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.varsler.SakForVarselvurdering
import java.time.LocalDateTime

interface SakVarselRepo {

    fun flaggForVarselvurdering(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    )

    fun hentSakerSomSkalVurdereVarsel(
        limit: Int = 25,
        sessionContext: SessionContext? = null,
    ): List<SakForVarselvurdering>

    /**
     * Markerer saken som vurdert (skal_vurdere_varsel = false) med optimistisk lås på
     * `sist_flagget_tidspunkt`.
     *
     * Oppdateringen gjøres kun dersom [sistFlaggetTidspunktVedLesing] matcher verdien i
     * databasen akkurat nå. Hvis en konkurrerende transaksjon har kalt
     * [flaggForVarselvurdering] etter at jobben leste saken, kaster vi [OptimistiskLåsFeil]
     * slik at varseljobbens transaksjon ruller tilbake. Saken forblir da flagget og plukkes
     * opp i neste kjøring med oppdatert datagrunnlag.
     */
    fun markerVarselVurdert(
        sakId: SakId,
        vurdertTidspunkt: LocalDateTime,
        sistFlaggetTidspunktVedLesing: LocalDateTime?,
        sessionContext: SessionContext? = null,
    )
}
