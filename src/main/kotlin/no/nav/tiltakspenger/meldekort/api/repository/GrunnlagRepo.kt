package no.nav.tiltakspenger.meldekort.api.repository

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag

interface GrunnlagRepo {
    fun lagre(dto: MeldekortGrunnlag)
    fun hentAktiveGrunnlagForInneværendePeriode(): List<MeldekortGrunnlag>

    fun hentForBehandling(behandlingId: String): MeldekortGrunnlag?

//    fun hent(id: String): MeldekortGrunnlagDTO?
//    fun hentAlleForBehandling(id: String): List<MeldekortGrunnlagDTO>
}
