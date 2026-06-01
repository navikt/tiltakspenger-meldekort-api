package no.nav.tiltakspenger.meldekort.meldeperiode.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldeperiodePostgresRepoTest {

    @Test
    fun `lagrer og henter meldeperiode for id`() {
        withMigratedDb(runIsolated = false) { helper ->
            val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(fixedClock))
            helper.sakPostgresRepo.lagre(
                ObjectMother.sak(
                    id = meldeperiode.sakId,
                    fnr = meldeperiode.fnr,
                    saksnummer = meldeperiode.saksnummer,
                ),
            )

            helper.meldeperiodeRepo.lagre(meldeperiode)

            helper.meldeperiodeRepo.hentForId(meldeperiode.id) shouldBe meldeperiode
        }
    }

    @Test
    fun `hentForId returnerer null for ukjent id`() {
        withMigratedDb(runIsolated = false) { helper ->
            helper.meldeperiodeRepo.hentForId(MeldeperiodeId.random()) shouldBe null
        }
    }

    @Test
    fun `henter siste meldeperiode for en kjede`() {
        withMigratedDb(runIsolated = false) { helper ->
            val meldeperiode1 = ObjectMother.meldeperiode(opprettet = nå(fixedClock))
            helper.sakPostgresRepo.lagre(
                ObjectMother.sak(
                    id = meldeperiode1.sakId,
                    fnr = meldeperiode1.fnr,
                    saksnummer = meldeperiode1.saksnummer,
                ),
            )
            helper.meldeperiodeRepo.lagre(meldeperiode1)
            helper.meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(meldeperiode1.kjedeId, meldeperiode1.fnr) shouldBe meldeperiode1
            val meldeperiode2 = ObjectMother.meldeperiode(
                periode = meldeperiode1.periode,
                fnr = meldeperiode1.fnr,
                saksnummer = meldeperiode1.saksnummer,
                sakId = meldeperiode1.sakId,
                versjon = meldeperiode1.versjon + 1,
                antallDagerForPeriode = meldeperiode1.antallDagerSomIkkeGirRett - 1, // Antall dager minker med 1.
                opprettet = meldeperiode1.opprettet.plusSeconds(1),
            )
            helper.meldeperiodeRepo.lagre(meldeperiode2)
            helper.meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(meldeperiode1.kjedeId, meldeperiode1.fnr) shouldBe meldeperiode2
        }
    }

    @Test
    fun `henter ikke siste meldeperiode for en kjede med feil fnr`() {
        withMigratedDb(runIsolated = false) { helper ->
            val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(fixedClock))
            helper.sakPostgresRepo.lagre(
                ObjectMother.sak(
                    id = meldeperiode.sakId,
                    fnr = meldeperiode.fnr,
                    saksnummer = meldeperiode.saksnummer,
                ),
            )
            helper.meldeperiodeRepo.lagre(meldeperiode)

            helper.meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(meldeperiode.kjedeId, helper.nesteFnr()) shouldBe null
        }
    }

    @Test
    fun `henter meldeperiode for en gitt periode`() {
        withMigratedDb(runIsolated = false) { helper ->
            val periode = ObjectMother.periode()
            val fnr = helper.nesteFnr()
            val meldeperiode = ObjectMother.meldeperiode(periode = periode, fnr = fnr, opprettet = nå(fixedClock))
            helper.sakPostgresRepo.lagre(
                ObjectMother.sak(
                    id = meldeperiode.sakId,
                    fnr = meldeperiode.fnr,
                    saksnummer = meldeperiode.saksnummer,
                ),
            )
            helper.meldeperiodeRepo.lagre(meldeperiode)
            val hentetMeldeperiode = helper.meldeperiodeRepo.hentMeldeperiodeForPeriode(periode, fnr)
            hentetMeldeperiode shouldBe meldeperiode
        }
    }

    @Test
    fun `henter nyeste versjon for en gitt periode`() {
        withMigratedDb(runIsolated = false) { helper ->
            val periode = ObjectMother.periode()
            val meldeperiode1 = ObjectMother.meldeperiode(periode = periode, opprettet = nå(fixedClock), versjon = 1)
            val meldeperiode2 = ObjectMother.meldeperiode(
                periode = periode,
                fnr = meldeperiode1.fnr,
                sakId = meldeperiode1.sakId,
                saksnummer = meldeperiode1.saksnummer,
                versjon = 2,
                opprettet = nå(fixedClock).plusSeconds(1),
            )
            helper.sakPostgresRepo.lagre(
                ObjectMother.sak(
                    id = meldeperiode1.sakId,
                    fnr = meldeperiode1.fnr,
                    saksnummer = meldeperiode1.saksnummer,
                ),
            )
            helper.meldeperiodeRepo.lagre(meldeperiode1)
            helper.meldeperiodeRepo.lagre(meldeperiode2)

            helper.meldeperiodeRepo.hentMeldeperiodeForPeriode(periode, meldeperiode1.fnr) shouldBe meldeperiode2
        }
    }

    @Test
    fun `henter ikke meldeperiode for periode med feil fnr`() {
        withMigratedDb(runIsolated = false) { helper ->
            val periode = ObjectMother.periode()
            val meldeperiode = ObjectMother.meldeperiode(periode = periode, opprettet = nå(fixedClock))
            helper.sakPostgresRepo.lagre(
                ObjectMother.sak(
                    id = meldeperiode.sakId,
                    fnr = meldeperiode.fnr,
                    saksnummer = meldeperiode.saksnummer,
                ),
            )
            helper.meldeperiodeRepo.lagre(meldeperiode)

            helper.meldeperiodeRepo.hentMeldeperiodeForPeriode(periode, helper.nesteFnr()) shouldBe null
        }
    }

    @Test
    fun `hentForSakId returnerer nyeste versjon per periode sortert på fra-og-med`() {
        withMigratedDb(runIsolated = false) { helper ->
            val førstePeriode = ObjectMother.periode()
            val meldeperiode1Versjon1 = ObjectMother.meldeperiode(periode = førstePeriode, opprettet = nå(fixedClock), versjon = 1)
            val meldeperiode1Versjon2 = ObjectMother.meldeperiode(
                periode = førstePeriode,
                fnr = meldeperiode1Versjon1.fnr,
                sakId = meldeperiode1Versjon1.sakId,
                saksnummer = meldeperiode1Versjon1.saksnummer,
                versjon = 2,
                opprettet = nå(fixedClock).plusSeconds(1),
            )
            val meldeperiode2 = ObjectMother.meldeperiode(
                periode = førstePeriode.plus14Dager(),
                fnr = meldeperiode1Versjon1.fnr,
                sakId = meldeperiode1Versjon1.sakId,
                saksnummer = meldeperiode1Versjon1.saksnummer,
                opprettet = nå(fixedClock).plusDays(14),
            )
            helper.sakPostgresRepo.lagre(
                ObjectMother.sak(
                    id = meldeperiode1Versjon1.sakId,
                    fnr = meldeperiode1Versjon1.fnr,
                    saksnummer = meldeperiode1Versjon1.saksnummer,
                ),
            )
            helper.meldeperiodeRepo.lagre(meldeperiode1Versjon1)
            helper.meldeperiodeRepo.lagre(meldeperiode1Versjon2)
            helper.meldeperiodeRepo.lagre(meldeperiode2)

            val meldeperioder = helper.sessionFactory.withSession { session ->
                MeldeperiodePostgresRepo.hentSisteMeldeperioderForSakId(meldeperiode1Versjon1.sakId, session)
            }

            meldeperioder shouldBe listOf(meldeperiode1Versjon2, meldeperiode2)
        }
    }
}
