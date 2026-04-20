package no.nav.tiltakspenger.meldekort.clients.arena

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr

interface ArenaMeldekortClient {
    suspend fun hentMeldekort(fnr: Fnr): Either<ArenaMeldekortServiceFeil, ArenaMeldekortResponse?>
    suspend fun hentHistoriskeMeldekort(fnr: Fnr, antallMeldeperioder: Int = 10): Either<ArenaMeldekortServiceFeil, ArenaMeldekortResponse?>
}
