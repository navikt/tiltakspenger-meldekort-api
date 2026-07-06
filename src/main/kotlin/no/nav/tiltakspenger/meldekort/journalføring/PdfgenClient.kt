package no.nav.tiltakspenger.meldekort.journalføring

import arrow.core.Either
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort

interface PdfgenClient {
    suspend fun genererMeldekortPdf(
        meldekort: BrukersMeldekort,
        errorContext: String = "SakId: ${meldekort.sakId}, saksnummer: ${meldekort.meldeperiode.saksnummer} meldekortId: ${meldekort.id}",
    ): Either<KunneIkkeGenererePdf, Pair<PdfOgJson, PdfOgJson?>>

    suspend fun genererKorrigertMeldekortPdf(
        meldekort: BrukersMeldekort,
        errorContext: String = "SakId: ${meldekort.sakId}, saksnummer: ${meldekort.meldeperiode.saksnummer} meldekortId: ${meldekort.id}",
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
