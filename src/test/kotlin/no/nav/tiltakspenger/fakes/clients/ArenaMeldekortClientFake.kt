package no.nav.tiltakspenger.fakes.clients

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortOversikt
import no.nav.tiltakspenger.testutils.uventetStatusFeil

class ArenaMeldekortClientFake : ArenaMeldekortClient {
    private val meldekortResponses = mutableMapOf<String, ArenaMeldekortOversikt>()
    private val historiskeMeldekortResponses = mutableMapOf<String, ArenaMeldekortOversikt>()
    private val meldekortFeil = mutableMapOf<String, HttpKlientError>()
    private val historiskeMeldekortFeil = mutableMapOf<String, HttpKlientError>()

    fun leggTilMeldekort(fnr: Fnr, response: ArenaMeldekortOversikt) {
        meldekortResponses[fnr.verdi] = response
    }

    fun leggTilHistoriskMeldekort(fnr: Fnr, response: ArenaMeldekortOversikt) {
        historiskeMeldekortResponses[fnr.verdi] = response
    }

    /** Gjør at [hentMeldekort] returnerer [feil] (Left) for denne brukeren. */
    fun leggTilMeldekortFeil(fnr: Fnr, feil: HttpKlientError = uventetStatusFeil().leftOrNull()!!) {
        meldekortFeil[fnr.verdi] = feil
    }

    /** Gjør at [hentHistoriskeMeldekort] returnerer [feil] (Left) for denne brukeren. */
    fun leggTilHistoriskMeldekortFeil(fnr: Fnr, feil: HttpKlientError = uventetStatusFeil().leftOrNull()!!) {
        historiskeMeldekortFeil[fnr.verdi] = feil
    }

    override suspend fun hentMeldekort(fnr: Fnr): Either<HttpKlientError, ArenaMeldekortOversikt?> {
        return meldekortFeil[fnr.verdi]?.left() ?: meldekortResponses[fnr.verdi].right()
    }

    override suspend fun hentHistoriskeMeldekort(
        fnr: Fnr,
        antallMeldeperioder: Int,
    ): Either<HttpKlientError, ArenaMeldekortOversikt?> {
        return historiskeMeldekortFeil[fnr.verdi]?.left() ?: historiskeMeldekortResponses[fnr.verdi].right()
    }
}
