package no.nav.tiltakspenger.fakes.clients

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortOversikt
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortServiceFeil

class ArenaMeldekortClientFake : ArenaMeldekortClient {
    private val meldekortResponses = mutableMapOf<String, ArenaMeldekortOversikt>()
    private val historiskeMeldekortResponses = mutableMapOf<String, ArenaMeldekortOversikt>()

    fun leggTilMeldekort(fnr: Fnr, response: ArenaMeldekortOversikt) {
        meldekortResponses[fnr.verdi] = response
    }

    fun leggTilHistoriskMeldekort(fnr: Fnr, response: ArenaMeldekortOversikt) {
        historiskeMeldekortResponses[fnr.verdi] = response
    }

    override suspend fun hentMeldekort(fnr: Fnr): Either<ArenaMeldekortServiceFeil, ArenaMeldekortOversikt?> {
        return meldekortResponses[fnr.verdi].right()
    }

    override suspend fun hentHistoriskeMeldekort(
        fnr: Fnr,
        antallMeldeperioder: Int,
    ): Either<ArenaMeldekortServiceFeil, ArenaMeldekortOversikt?> {
        return historiskeMeldekortResponses[fnr.verdi].right()
    }
}
