package no.nav.tiltakspenger.meldekort.arena

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError

interface ArenaMeldekortClient {
    suspend fun hentMeldekort(fnr: Fnr): Either<HttpKlientError, ArenaMeldekortOversikt?>
    suspend fun hentHistoriskeMeldekort(fnr: Fnr, antallMeldeperioder: Int = 10): Either<HttpKlientError, ArenaMeldekortOversikt?>
}
