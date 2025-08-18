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
            val meldeperiode1 = ObjectMother.meldeperiode()
            helper.meldeperiodeRepo.lagre(meldeperiode1)
            helper.meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(meldeperiode1.kjedeId, meldeperiode1.fnr) shouldBe meldeperiode1
            val meldeperiode2 = ObjectMother.meldeperiode(
                periode = meldeperiode1.periode,
                fnr = meldeperiode1.fnr,
                saksnummer = meldeperiode1.saksnummer,
                sakId = meldeperiode1.sakId,
                versjon = meldeperiode1.versjon + 1,
                antallDagerForPeriode = meldeperiode1.antallDagerIkkeRett - 1, // Antall dager minker med 1.
                opprettet = meldeperiode1.opprettet.plusSeconds(1),
            )
            helper.meldeperiodeRepo.lagre(meldeperiode2)
            helper.meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(meldeperiode1.kjedeId, meldeperiode1.fnr) shouldBe meldeperiode2
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
