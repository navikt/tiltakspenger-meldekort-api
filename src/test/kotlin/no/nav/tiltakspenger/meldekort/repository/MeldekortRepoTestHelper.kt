package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.meldekort.domene.Meldekort

fun lagreMeldekort(helper: TestDataHelper, vararg meldekort: Meldekort) {
    val meldeperiodeRepo = helper.meldeperiodeRepo
    val meldekortRepo = helper.meldekortPostgresRepo

    meldekort.forEach {
        meldeperiodeRepo.lagre(it.meldeperiode)
        meldekortRepo.lagre(it)
    }
}
