package no.nav.tiltakspenger.meldekort.landingsside.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideMeldekort
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideSak
import no.nav.tiltakspenger.meldekort.meldekort.infra.lagreMeldekort
import no.nav.tiltakspenger.meldekort.meldekortvedtak.infra.lagreMeldekortvedtak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class LandingssidePostgresRepoTest {

    @Test
    fun `hentSak returnerer null for ukjent fnr`() {
        withMigratedDb(runIsolated = false) { helper ->
            helper.landingssidePostgresRepo.hentSak(helper.nesteFnr()) shouldBe null
        }
    }

    @Test
    fun `hentSak returnerer sak uten meldekort`() {
        withMigratedDb(runIsolated = false) { helper ->
            val fnr = helper.nesteFnr()
            helper.sakPostgresRepo.lagre(
                ObjectMother.sak(
                    fnr = fnr,
                    saksnummer = helper.nesteSaksnummer(),
                    arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
                ),
            )

            helper.landingssidePostgresRepo.hentSak(fnr) shouldBe LandingssideSak(
                fnr = fnr,
                arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = emptyList(),
            )
        }
    }

    @Test
    fun `hentSak mapper HAR_MELDEKORT fra db`() {
        withMigratedDb(runIsolated = false) { helper ->
            val fnr = helper.nesteFnr()
            helper.sakPostgresRepo.lagre(
                ObjectMother.sak(
                    fnr = fnr,
                    saksnummer = helper.nesteSaksnummer(),
                    arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT,
                ),
            )

            helper.landingssidePostgresRepo.hentSak(fnr) shouldBe LandingssideSak(
                fnr = fnr,
                arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT,
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = emptyList(),
            )
        }
    }

    @Test
    fun `hentSak returnerer innsendt-status og bare meldekort som er klare til utfylling`() {
        val clock = ObjectMother.tikkendeKlokke1mars2025()
        withMigratedDb(runIsolated = false, clock = clock) { helper ->
            val fnr = helper.nesteFnr()
            val sakId = SakId.random()
            val saksnummer = helper.nesteSaksnummer()
            val førsteKlareMeldekort = ObjectMother.meldekort(
                periode = Periode(6.januar(2025), 19.januar(2025)),
                mottatt = null,
                fnr = fnr,
                sakId = sakId,
                saksnummer = saksnummer,
                opprettet = nå(clock),
            )
            val innsendtMeldekort = ObjectMother.meldekort(
                periode = Periode(20.januar(2025), 2.februar(2025)),
                mottatt = nå(clock),
                fnr = fnr,
                sakId = sakId,
                saksnummer = saksnummer,
                opprettet = nå(clock),
            )
            val andreKlareMeldekort = ObjectMother.meldekort(
                periode = Periode(3.februar(2025), 16.februar(2025)),
                mottatt = null,
                fnr = fnr,
                sakId = sakId,
                saksnummer = saksnummer,
                opprettet = nå(clock),
            )
            val deaktivertMeldekort = ObjectMother.meldekort(
                periode = Periode(17.februar(2025), 2.mars(2025)),
                mottatt = null,
                deaktivert = nå(clock),
                fnr = fnr,
                sakId = sakId,
                saksnummer = saksnummer,
                opprettet = nå(clock),
            )
            val vedtaksbehandletMeldekort = ObjectMother.meldekort(
                periode = Periode(9.desember(2024), 22.desember(2024)),
                mottatt = null,
                fnr = fnr,
                sakId = sakId,
                saksnummer = saksnummer,
                opprettet = nå(clock),
            )
            val fremtidigMeldekort = ObjectMother.meldekort(
                periode = Periode(17.mars(2025), 30.mars(2025)),
                mottatt = null,
                fnr = fnr,
                sakId = sakId,
                saksnummer = saksnummer,
                opprettet = nå(clock),
            )
            lagreMeldekort(
                helper,
                andreKlareMeldekort,
                fremtidigMeldekort,
                førsteKlareMeldekort,
                deaktivertMeldekort,
                innsendtMeldekort,
                vedtaksbehandletMeldekort,
            )
            helper.meldekortPostgresRepo.deaktiver(deaktivertMeldekort.id)
            lagreMeldekortvedtak(helper, ObjectMother.meldekortvedtak(vedtaksbehandletMeldekort))

            helper.landingssidePostgresRepo.hentSak(fnr) shouldBe LandingssideSak(
                fnr = fnr,
                arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
                harInnsendteMeldekort = true,
                meldekortTilUtfylling = listOf(
                    LandingssideMeldekort(førsteKlareMeldekort.meldeperiode.kanFyllesUtFraOgMed),
                    LandingssideMeldekort(andreKlareMeldekort.meldeperiode.kanFyllesUtFraOgMed),
                ),
            )
        }
    }

    @Test
    fun `hentSak teller meldekort med vedtak som innsendt selv uten mottatt`() {
        val clock = ObjectMother.tikkendeKlokke1mars2025()
        withMigratedDb(runIsolated = false, clock = clock) { helper ->
            val fnr = helper.nesteFnr()
            val sakId = SakId.random()
            val saksnummer = helper.nesteSaksnummer()
            val vedtaksbehandletMeldekort = ObjectMother.meldekort(
                periode = Periode(9.desember(2024), 22.desember(2024)),
                mottatt = null,
                fnr = fnr,
                sakId = sakId,
                saksnummer = saksnummer,
                opprettet = nå(clock),
            )
            lagreMeldekort(helper, vedtaksbehandletMeldekort)
            lagreMeldekortvedtak(helper, ObjectMother.meldekortvedtak(vedtaksbehandletMeldekort))

            helper.landingssidePostgresRepo.hentSak(fnr) shouldBe LandingssideSak(
                fnr = fnr,
                arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
                harInnsendteMeldekort = true,
                meldekortTilUtfylling = emptyList(),
            )
        }
    }

    @Test
    fun `hentSak teller papirmeldekort (vedtak uten bruker-meldekort) som innsendt`() {
        val clock = ObjectMother.tikkendeKlokke1mars2025()
        withMigratedDb(runIsolated = false, clock = clock) { helper ->
            val fnr = helper.nesteFnr()
            val sakId = SakId.random()
            val saksnummer = helper.nesteSaksnummer()
            val papirmeldekort = ObjectMother.meldekort(
                periode = Periode(9.desember(2024), 22.desember(2024)),
                mottatt = null,
                fnr = fnr,
                sakId = sakId,
                saksnummer = saksnummer,
                opprettet = nå(clock),
            )
            // Papirmeldekort behandles i saksbehandling-api og kommer hit kun som meldekortvedtak.
            // Det finnes altså en sak og en meldeperiode, men ingen meldekort_bruker-rad.
            helper.sakPostgresRepo.lagre(
                ObjectMother.sak(id = sakId, fnr = fnr, saksnummer = saksnummer),
            )
            helper.meldeperiodeRepo.lagre(papirmeldekort.meldeperiode)
            lagreMeldekortvedtak(helper, ObjectMother.meldekortvedtak(papirmeldekort))

            helper.landingssidePostgresRepo.hentSak(fnr) shouldBe LandingssideSak(
                fnr = fnr,
                arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
                harInnsendteMeldekort = true,
                meldekortTilUtfylling = emptyList(),
            )
        }
    }
}
