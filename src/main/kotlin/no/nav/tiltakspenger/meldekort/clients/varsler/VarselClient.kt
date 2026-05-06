package no.nav.tiltakspenger.meldekort.clients.varsler

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.domene.VarselId
import java.time.LocalDateTime

interface VarselClient {
    /**
     * Bygger en varselhendelse uten å publisere på Kafka. Idempotent og fri for sideeffekter, og
     * trygg å kalle utenfor transaksjon. Returnerer json-payloaden vi senere skal sende – denne
     * lagres som `aktiveringsmetadata` slik at vi har audit-trail i databasen.
     *
     * @param utsettSendingTil Tidspunktet Min side skal sende varselet til bruker på. `null`
     *   betyr at varselet skal sendes umiddelbart. Eies av domenet (typisk
     *   [no.nav.tiltakspenger.meldekort.domene.varsler.Varsel.skalAktiveresTidspunkt]) – klienten
     *   har ingen egen tidsberegning.
     */
    fun byggNyttMeldekortVarsel(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: LocalDateTime?,
    ): SendtVarselMetadata

    /**
     * Bygger en enklere beskjed for tilfeller der endringer på rammevedtak har gitt en ny versjon
     * av meldeperioden. Bruker kan selv deaktivere/lukke denne på Min side.
     */
    fun byggMeldeperiodeEndretBeskjed(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: LocalDateTime?,
    ): SendtVarselMetadata

    /**
     * Publiserer en allerede bygd varselhendelse på Kafka. Skal kalles innenfor samme transaksjon
     * som [no.nav.tiltakspenger.meldekort.repository.VarselRepo.lagre], og *etter* lagre-kallet,
     * slik at vi ikke produserer på Kafka dersom optimistisk lås slår til.
     */
    fun sendVarsel(varselId: VarselId, metadata: SendtVarselMetadata)

    /**
     * Publiserer en inaktivering av et varsel på Kafka. Skal kalles innenfor samme transaksjon
     * som [no.nav.tiltakspenger.meldekort.repository.VarselRepo.lagre], og *etter* lagre-kallet,
     * slik at vi ikke produserer på Kafka dersom optimistisk lås slår til.
     */
    fun inaktiverVarsel(varselId: VarselId)
}
