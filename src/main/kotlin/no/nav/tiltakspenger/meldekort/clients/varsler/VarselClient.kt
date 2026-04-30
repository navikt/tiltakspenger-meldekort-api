package no.nav.tiltakspenger.meldekort.clients.varsler

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.domene.VarselId
import java.time.LocalDateTime

interface VarselClient {
    /**
     * @param utsettSendingTil Tidspunktet Min side skal sende varselet til bruker på. `null` betyr at
     *   varselet skal sendes umiddelbart. Eies av domenet (typisk
     *   [no.nav.tiltakspenger.meldekort.domene.varsler.Varsel.skalAktiveresTidspunkt]) – klienten
     *   har ingen egen tidsberegning.
     */
    fun sendVarsel(varselId: VarselId, fnr: Fnr, utsettSendingTil: LocalDateTime?): SendtVarselMetadata
    fun inaktiverVarsel(varselId: VarselId)
}
