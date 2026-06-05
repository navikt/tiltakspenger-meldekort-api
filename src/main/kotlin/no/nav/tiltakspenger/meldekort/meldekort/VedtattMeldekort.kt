package no.nav.tiltakspenger.meldekort.meldekort

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import java.time.LocalDateTime

/**
 * Registrert tilstand avledet fra et meldekortvedtak (papirmeldekort / saksbehandler-behandlet
 * meldeperiode) for en kjede som mangler en digital brukerinnsending.
 *
 * Brukes kun på lesesiden mot bruker («allerede utfylt» + korrigering). Persisteres aldri.
 *
 * @param id Korrigeringsmål — id-en til kjedens åpne (uinnsendte) [BrukersMeldekort]. Et meldekortvedtak
 *  for en papir-only meldeperiode har ikke `brukersMeldekortId`, så korrigeringsmålet hentes fra det
 *  åpne meldekortet som ble opprettet på mottak.
 * @param meldeperiode Meldeperioden tilstanden hører til (kjedens åpne meldekort sin meldeperiode).
 * @param dager Status per dag slik den ble vedtatt.
 * @param opprettet Tidspunktet vedtaket ble opprettet — brukes som registrert-/«innsendt»-tidspunkt.
 */
data class VedtattMeldekort(
    override val id: MeldekortId,
    override val meldeperiode: Meldeperiode,
    override val dager: List<MeldekortDag>,
    val opprettet: LocalDateTime,
) : RegistrertMeldekort
