package no.nav.tiltakspenger.meldekort.journalføring

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort

interface PdfgenClient {
    suspend fun genererMeldekortPdf(
        meldekort: BrukersMeldekort,
    ): Either<HttpKlientError, Pair<PdfOgJson, PdfOgJson?>>

    suspend fun genererKorrigertMeldekortPdf(
        meldekort: BrukersMeldekort,
    ): Either<HttpKlientError, Pair<PdfOgJson, PdfOgJson?>>
}
