package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.Meldekort

interface MeldekortService {

    fun lagreMeldekort(meldekort: Meldekort)

    fun hentMeldekort(meldekortId: MeldekortId): Meldekort?

    fun hentSisteMeldekort(fnr: Fnr): Meldekort?

    fun hentAlleMeldekort(fnr: Fnr): List<Meldekort>
}
