package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.meldekort.domene.Meldekort

interface MeldekortService {

    fun lagreMeldekort(meldekort: Meldekort)

    fun hentMeldekort(id: String): Meldekort?

    fun hentSisteMeldekort(fnr: String): Meldekort?

    fun hentAlleMeldekort(fnr: String): List<Meldekort>
}
