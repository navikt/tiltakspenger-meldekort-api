package no.nav.tiltakspenger.fakes.clients

import ArenaMeldekortClient
import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekortResponse
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekortServiceFeil

class ArenaMeldekortClientFake : ArenaMeldekortClient {
    private val meldekortResponses = mutableMapOf<String, ArenaMeldekortResponse>()

    fun leggTilMeldekort(fnr: Fnr, response: ArenaMeldekortResponse) {
        meldekortResponses[fnr.verdi] = response
    }

    override suspend fun hentMeldekort(fnr: Fnr): Either<ArenaMeldekortServiceFeil, ArenaMeldekortResponse?> {
        return meldekortResponses[fnr.verdi].right()
    }

    override suspend fun hentHistoriskeMeldekort(
        fnr: Fnr,
        antallMeldeperioder: Int,
    ): Either<ArenaMeldekortServiceFeil, ArenaMeldekortResponse?> {
        return meldekortResponses[fnr.verdi].right()
    }
}
