package no.nav.tiltakspenger.meldekort.repository

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldeperiodePostgresRepoTest {

    @Test
    fun `henter siste meldeperiode for en kjede`() {
        withMigratedDb { helper ->
            val meldeperiode = ObjectMother.meldeperiode()
            val kjedeId = meldeperiode.kjedeId
            helper.meldeperiodeRepo.lagre(meldeperiode)
            val hentetMeldeperiode =
                helper.meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(kjedeId, meldeperiode.fnr)
            hentetMeldeperiode shouldBe meldeperiode
        }
    }

    @Test
    fun `henter meldeperiode for en gitt periode`() {
        withMigratedDb { helper ->
            val periode = ObjectMother.periode()
            val fnr = Fnr.random()
            val meldeperiode = ObjectMother.meldeperiode(periode = periode, fnr = fnr)
            helper.meldeperiodeRepo.lagre(meldeperiode)
            val hentetMeldeperiode = helper.meldeperiodeRepo.hentMeldeperiodeForPeriode(periode, fnr)
            hentetMeldeperiode shouldBe meldeperiode
        }
    }
}
