package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.TransactionalSession
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortUtenDager
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import java.util.UUID

interface MeldekortRepo {
    fun opprett(grunnlagId: UUID, meldekort: Meldekort.Åpent)

    fun hentMeldekortForGrunnlag(grunnlagId: UUID): List<MeldekortUtenDager>

    fun hentPerioderForMeldekortForGrunnlag(grunnlagId: UUID): List<Periode>

    fun hentMeldekortMedId(meldekortId: UUID): Meldekort?

    fun hentGrunnlagIdForMeldekort(meldekortId: UUID): UUID?

    fun lagreInnsendtMeldekort(meldekort: Meldekort.Innsendt)

    fun lagreInnsendtMeldekort(meldekort: Meldekort.Innsendt, tx: TransactionalSession)

    fun lagreJournalPostId(jornalpostId: String, meldekortId: UUID)

    fun lagreJournalPostId(jornalpostId: String, meldekortId: UUID, tx: TransactionalSession)
}
