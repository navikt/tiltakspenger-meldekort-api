package no.nav.tiltakspenger.meldekort.api.repository

import no.nav.tiltakspenger.meldekort.api.dto.Meldekort
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortMedTiltak

interface MeldekortRepo {
    fun lagre(meldekort: Meldekort.Registrert)

    fun hent(id: String): MeldekortMedTiltak?

    fun hentAlle(ident: String): List<MeldekortMedTiltak>
}
