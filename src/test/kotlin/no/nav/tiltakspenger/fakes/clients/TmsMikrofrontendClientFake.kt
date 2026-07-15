package no.nav.tiltakspenger.fakes.clients

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendFeil
import no.nav.tiltakspenger.meldekort.microfrontend.TmsMikrofrontendClient

/**
 * Recording-fake for [TmsMikrofrontendClient].
 * Registrerer aktiverte/inaktiverte brukere slik at tester kan verifisere mot faken i stedet for mot en mock.
 *
 * Bruk [kastFeilFor] for å simulere at kallet mot det eksterne systemet feiler (returnerer [MicrofrontendFeil.KafkaFeil]) for en gitt sak.
 */
class TmsMikrofrontendClientFake : TmsMikrofrontendClient {
    private val logger = KotlinLogging.logger {}
    private val aktiverte = mutableListOf<MicrofrontendBruker>()
    private val inaktiverte = mutableListOf<MicrofrontendBruker>()
    private val feilForSakId = mutableSetOf<SakId>()

    fun kastFeilFor(sakId: SakId) {
        feilForSakId.add(sakId)
    }

    fun aktiverte(): List<MicrofrontendBruker> = aktiverte.toList()

    fun inaktiverte(): List<MicrofrontendBruker> = inaktiverte.toList()

    override fun aktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId): Either<MicrofrontendFeil, Unit> {
        if (sakId in feilForSakId) return MicrofrontendFeil.KafkaFeil(RuntimeException("simulert kafka-feil")).left()
        logger.info { "Aktiverer (ikke) mikrofrontend for sakId=$sakId" }
        aktiverte.add(MicrofrontendBruker(fnr, sakId))
        return Unit.right()
    }

    override fun inaktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId): Either<MicrofrontendFeil, Unit> {
        if (sakId in feilForSakId) return MicrofrontendFeil.KafkaFeil(RuntimeException("simulert kafka-feil")).left()
        logger.info { "Inaktiverer (ikke) mikrofrontend for sakId=$sakId" }
        inaktiverte.add(MicrofrontendBruker(fnr, sakId))
        return Unit.right()
    }

    data class MicrofrontendBruker(val fnr: Fnr, val sakId: SakId)
}
