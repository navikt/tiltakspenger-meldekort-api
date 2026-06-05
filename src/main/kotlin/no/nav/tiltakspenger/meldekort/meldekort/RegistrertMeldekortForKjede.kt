package no.nav.tiltakspenger.meldekort.meldekort

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import java.time.Clock
import java.time.LocalDateTime

/**
 * Slår sammen brukers digitale meldekort for en kjede ([MeldekortForKjede]) med eventuelle
 * meldekortvedtak (papirmeldekort / saksbehandler-behandlede meldeperioder) for samme kjede.
 *
 * Brukes av bruker-flaten der vi må ta høyde for at en kjede kan være «utfylt» enten digitalt eller via et meldekortvedtak.
 * De digital utfylte meldekortene skal alltid ha prioritet (brukes til visning og pre-utfylling av korrigering).
 */
data class RegistrertMeldekortForKjede(
    val meldekortForKjede: MeldekortForKjede,
    private val meldekortvedtak: List<Meldekortvedtak>,
) {
    val kjedeId = meldekortForKjede.kjedeId

    private data class VedtakTilstand(
        val vedtakOpprettetTidspunkt: LocalDateTime,
        val meldeperiodeId: MeldeperiodeId,
        val dager: List<MeldekortDag>,
    )

    private val sistePapirOnlyVedtakForKjede: VedtakTilstand? by lazy {
        meldekortvedtak
            .flatMap { vedtak -> vedtak.meldeperiodebehandlinger.map { vedtak.opprettet to it } }
            .filter { (_, behandling) -> behandling.meldeperiodeKjedeId == kjedeId }
            // Kun papir-only behandlinger (uten brukersMeldekortId) er en selvstendig registrert tilstand.
            // Behandlinger basert på brukers innsending (brukersMeldekortId satt) representeres av brukers
            // eget digitale meldekort, og skal ikke overstyre korrigeringsutgangspunktet.
            .filter { (_, behandling) -> behandling.brukersMeldekortId == null }
            .maxByOrNull { (opprettet, _) -> opprettet }
            ?.let { (opprettet, behandling) ->
                VedtakTilstand(
                    vedtakOpprettetTidspunkt = opprettet,
                    meldeperiodeId = behandling.meldeperiodeId,
                    dager = behandling.dager.map { MeldekortDag(dag = it.dato, status = it.status) },
                )
            }
    }

    /**
     * Kjedens siste registrerte tilstand for bruker-flaten.
     *
     * Brukers digitale innsending er alltid korrigeringsutgangspunktet når den finnes — også om
     * saksbehandler senere har behandlet/overstyrt meldeperioden. Et meldekortvedtak brukes som
     * registrert tilstand kun for papir-only kjeder (uten en digital innsending, dvs.
     * brukersMeldekortId == null). Null dersom kjeden verken har en digital innsending eller et
     * papir-only meldekortvedtak.
     */
    fun sisteRegistrerte(): RegistrertMeldekort? {
        // Brukers digitale innsending vinner alltid når den finnes.
        meldekortForKjede.sisteInnsendteMeldekort()?.let { return it }

        // Ingen digital innsending: bruk et eventuelt papir-only meldekortvedtak for kjeden.
        val vedtak = sistePapirOnlyVedtakForKjede ?: return null
        // Korrigeringsmål for en papir-only kjede er kjedens åpne (uinnsendte) meldekort.
        val korrigeringsmaal = meldekortForKjede.lastOrNull() ?: return null
        val meldeperiode = meldeperiodeForVedtak(vedtak.meldeperiodeId) ?: korrigeringsmaal.meldeperiode
        return VedtattMeldekort(
            id = korrigeringsmaal.id,
            meldeperiode = meldeperiode,
            dager = vedtak.dager,
            opprettet = vedtak.vedtakOpprettetTidspunkt,
        )
    }

    fun kanKorrigeres(meldekortId: MeldekortId): Boolean = sisteRegistrerte()?.id == meldekortId

    /**
     * Korrigerer kjedens siste registrerte tilstand (digital eller meldekortvedtak).
     *
     * - Digital innsending: lager (som før) et nytt/oppdatert [BrukersMeldekort] for [sisteMeldeperiode].
     * - Papir-only meldekortvedtak: lager alltid et NYTT [BrukersMeldekort] for [sisteMeldeperiode], og
     *   signaliserer at kjedens åpne placeholder-meldekort skal deaktiveres
     *   ([KorrigeringResultat.placeholderSomDeaktiveres]). Placeholderen fylles aldri ut in-place.
     */
    fun korriger(
        command: KorrigerMeldekortCommand,
        sisteMeldeperiode: Meldeperiode,
        clock: Clock,
    ): Either<FeilVedKorrigeringAvMeldekort, KorrigeringResultat> {
        val registrert = sisteRegistrerte()
            ?: return FeilVedKorrigeringAvMeldekort.IkkeSisteMeldekortIKjeden.left()

        if (command.meldekortId != registrert.id) {
            return FeilVedKorrigeringAvMeldekort.IkkeSisteMeldekortIKjeden.left()
        }

        return when (registrert) {
            is BrukersMeldekort -> KorrigeringResultat(
                korrigertMeldekort = meldekortForKjede.byggKorrigertMeldekort(command, sisteMeldeperiode, clock),
                placeholderSomDeaktiveres = null,
            ).right()

            is VedtattMeldekort -> KorrigeringResultat(
                korrigertMeldekort = meldekortForKjede.byggNyttKorrigertMeldekort(command, sisteMeldeperiode, clock),
                placeholderSomDeaktiveres = registrert.id,
            ).right()
        }
    }

    private fun meldeperiodeForVedtak(
        meldeperiodeId: MeldeperiodeId,
    ): Meldeperiode? = meldekortForKjede.firstOrNull { it.meldeperiode.id == meldeperiodeId }?.meldeperiode
}

/**
 * Resultat av en korrigering på en meldeperiodekjede.
 *
 * @property korrigertMeldekort det nye/oppdaterte meldekortet som skal lagres.
 * @property placeholderSomDeaktiveres id-en til kjedens åpne placeholder-meldekort som skal deaktiveres
 * i samme transaksjon (kun satt ved korrigering av en papir-only kjede), ellers null.
 */
data class KorrigeringResultat(
    val korrigertMeldekort: BrukersMeldekort,
    val placeholderSomDeaktiveres: MeldekortId?,
)
