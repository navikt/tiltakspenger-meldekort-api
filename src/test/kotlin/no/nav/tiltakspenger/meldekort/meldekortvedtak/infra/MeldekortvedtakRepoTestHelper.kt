package no.nav.tiltakspenger.meldekort.meldekortvedtak.infra

import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.lagreMeldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak

/**
 * Lagrer meldekortvedtak via [TestDataHelper].
 *
 * Forutsetter at saken (FK `sak_id`) allerede finnes - typisk opprettet via
 * [no.nav.tiltakspenger.meldekort.meldekort.infra.lagreMeldekort].
 */
fun lagreMeldekortvedtak(helper: TestDataHelper, vararg meldekortvedtak: Meldekortvedtak) {
    meldekortvedtak.forEach {
        helper.lagreMeldekortvedtak(it, null)
    }
}
