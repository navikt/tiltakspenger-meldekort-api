package no.nav.tiltakspenger.meldekort.journalføring

import arrow.core.Either
import no.nav.tiltakspenger.meldekort.meldekort.Meldekort

interface PdfgenClient {
    suspend fun genererMeldekortPdf(
        meldekort: Meldekort,
        errorContext: String = "SakId: ${meldekort.sakId}, saksnummer: ${meldekort.meldeperiode.saksnummer} meldekortId: ${meldekort.id}",
    ): Either<KunneIkkeGenererePdf, PdfOgJson>

    suspend fun genererKorrigertMeldekortPdf(
        meldekort: Meldekort,
        errorContext: String = "SakId: ${meldekort.sakId}, saksnummer: ${meldekort.meldeperiode.saksnummer} meldekortId: ${meldekort.id}",
    ): Either<KunneIkkeGenererePdf, PdfOgJson>
}
