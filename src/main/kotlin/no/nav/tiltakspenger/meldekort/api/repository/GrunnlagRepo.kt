package no.nav.tiltakspenger.meldekort.api.repository

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import java.util.UUID

interface GrunnlagRepo {
    fun lagre(
        dto: MeldekortGrunnlag,
        transactionContext: TransactionContext? = null,
    )

    fun hentAktiveGrunnlagForInneværendePeriode(): List<MeldekortGrunnlag>

    fun hentForBehandling(behandlingId: String): MeldekortGrunnlag?

    fun hentGrunnlag(id: UUID): MeldekortGrunnlag?

    fun hentGrunnlagForVedtakId(vedtakId: String): MeldekortGrunnlag?

//    fun hent(id: String): MeldekortGrunnlagDTO?
//    fun hentAlleForBehandling(id: String): List<MeldekortGrunnlagDTO>
}
