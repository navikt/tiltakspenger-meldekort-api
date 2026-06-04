package no.nav.tiltakspenger.meldekort.meldekort.infra

import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.lagreMeldeperiode
import no.nav.tiltakspenger.lagreSak
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.objectmothers.ObjectMother

fun lagreMeldekort(helper: TestDataHelper, vararg meldekort: BrukersMeldekort) {
    val meldeperiodeRepo = helper.meldeperiodeRepo
    val meldekortRepo = helper.meldekortPostgresRepo
    val sakRepo = helper.sakPostgresRepo

    meldekort.forEach {
        if (sakRepo.hent(it.sakId) == null) {
            helper.lagreSak(
                ObjectMother.sak(
                    id = it.sakId,
                    fnr = it.fnr,
                    saksnummer = it.saksnummer,
                ),
            )
        }
        if (meldeperiodeRepo.hentForId(it.meldeperiode.id) == null) {
            helper.lagreMeldeperiode(it.meldeperiode)
        }
        meldekortRepo.lagre(it)
    }
}
