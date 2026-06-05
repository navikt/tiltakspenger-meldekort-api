package no.nav.tiltakspenger.meldekort.meldekort

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import java.time.LocalDateTime

/**
 * Et «registrert» (utfylt) meldekort for en meldeperiode-kjede, slik det ser ut mot bruker —
 * uavhengig av kilde. To varianter:
 *  - [BrukersMeldekort]: brukers digitale innsending (meldekort-api er master).
 *  - [VedtattMeldekort]: tilstand avledet fra et meldekortvedtak (papirmeldekort / saksbehandler-behandlet
 *    meldeperiode) for en kjede uten digital innsending.
 *
 * For bruker-flaten («allerede utfylt» + korrigering) behandles begge kilder likt. [id] er alltid
 * en [BrukersMeldekort]-id, slik at korrigerings-routene har et entydig korrigeringsmål.
 */
sealed interface RegistrertMeldekort {
    val id: MeldekortId
    val meldeperiode: Meldeperiode
    val dager: List<MeldekortDag>
}

/**
 * Tidspunktet tilstanden ble registrert: brukerens `mottatt` for en digital innsending,
 * eller vedtakets `opprettet` for et [VedtattMeldekort]. En registrert tilstand er per definisjon
 * innsendt, så et [BrukersMeldekort] her må ha `mottatt != null`.
 */
fun RegistrertMeldekort.registrertTidspunkt(): LocalDateTime = when (this) {
    is BrukersMeldekort -> requireNotNull(mottatt) {
        "Et registrert BrukersMeldekort må være innsendt (mottatt != null). id: $id"
    }

    is VedtattMeldekort -> opprettet
}
