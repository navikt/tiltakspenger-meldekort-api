package no.nav.tiltakspenger.meldekort.repository.varsel

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.repository.lagreMeldekort
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VarselMeldekortPostgresRepoTest {

    private val sakId = SakId.random()
    private val saksnummer = Math.random().toString()
    private val fnr = Fnr.fromString(ObjectMother.FAKE_FNR)
    private val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

    @Nested
    inner class HentKjederSomManglerInnsending {

        @Test
        fun `returnerer kjede der nyeste meldeperiode ikke har innsendt meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val meldekort = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = null,
                    periode = førstePeriode,
                )
                lagreMeldekort(helper, meldekort)

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                requireNotNull(resultat).also {
                    it.sakId shouldBe sakId
                    it.kjedeId shouldBe meldekort.meldeperiode.kjedeId
                    it.kanFyllesUtFraOgMed shouldBe meldekort.meldeperiode.kanFyllesUtFraOgMed
                }
            }
        }

        @Test
        fun `returnerer null når meldekort er innsendt for nyeste versjon`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val meldekort = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = nå(fixedClock),
                    periode = førstePeriode,
                )
                lagreMeldekort(helper, meldekort)

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                resultat shouldBe null
            }
        }

        @Test
        fun `returnerer null når revurdering (versjon 2) mangler innsending men versjon 1 var innsendt`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                // Versjon 1 med innsendt meldekort
                val meldeperiodeV1 = ObjectMother.meldeperiode(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    versjon = 1,
                    periode = førstePeriode,
                    opprettet = nå(fixedClock),
                )
                val meldekortV1 = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = nå(fixedClock),
                    meldeperiode = meldeperiodeV1,
                )
                lagreSak(helper, meldeperiodeV1)
                helper.meldeperiodeRepo.lagre(meldeperiodeV1)
                helper.meldekortPostgresRepo.lagre(meldekortV1)

                // Versjon 2 (revurdering) uten innsendt meldekort
                val meldeperiodeV2 = meldeperiodeV1.copy(
                    id = MeldeperiodeId.random(),
                    versjon = 2,
                    opprettet = nå(fixedClock).plusHours(1),
                )
                val meldekortV2 = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = null,
                    meldeperiode = meldeperiodeV2,
                )
                helper.meldeperiodeRepo.lagre(meldeperiodeV2)
                helper.meldekortPostgresRepo.lagre(meldekortV2)

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                resultat shouldBe null
            }
        }

        @Test
        fun `returnerer null når revurdering (versjon 2) har innsendt meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val meldeperiodeV1 = ObjectMother.meldeperiode(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    versjon = 1,
                    periode = førstePeriode,
                    opprettet = nå(fixedClock),
                )
                lagreSak(helper, meldeperiodeV1)
                helper.meldeperiodeRepo.lagre(meldeperiodeV1)
                helper.meldekortPostgresRepo.lagre(
                    ObjectMother.meldekort(
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        mottatt = nå(fixedClock),
                        meldeperiode = meldeperiodeV1,
                    ),
                )

                val meldeperiodeV2 = meldeperiodeV1.copy(
                    id = MeldeperiodeId.random(),
                    versjon = 2,
                    opprettet = nå(fixedClock).plusHours(1),
                )
                helper.meldeperiodeRepo.lagre(meldeperiodeV2)
                helper.meldekortPostgresRepo.lagre(
                    ObjectMother.meldekort(
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        mottatt = nå(fixedClock).plusHours(2),
                        meldeperiode = meldeperiodeV2,
                    ),
                )

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                resultat shouldBe null
            }
        }

        @Test
        fun `returnerer null når nyeste meldeperiode har maks_antall_dager_for_periode lik 0`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                // Meldeperiode uten rett - det opprettes aldri et meldekort for denne
                val meldeperiode = ObjectMother.meldeperiode(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    versjon = 1,
                    periode = førstePeriode,
                    opprettet = nå(fixedClock),
                    girRett = førstePeriode.tilDager().associateWith { false },
                    antallDagerForPeriode = 0,
                )
                lagreSak(helper, meldeperiode)
                helper.meldeperiodeRepo.lagre(meldeperiode)

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                resultat shouldBe null
            }
        }

        @Test
        fun `returnerer kjede når nyeste meldeperiode mangler meldekort helt`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val meldeperiode = ObjectMother.meldeperiode(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    versjon = 1,
                    periode = førstePeriode,
                    opprettet = nå(fixedClock),
                )
                lagreSak(helper, meldeperiode)
                helper.meldeperiodeRepo.lagre(meldeperiode)

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                requireNotNull(resultat).also {
                    it.meldeperiodeId shouldBe meldeperiode.id
                    it.kjedeId shouldBe meldeperiode.kjedeId
                }
            }
        }

        @Test
        fun `returnerer kjede når kanFyllesUtFraOgMed er frem i tid`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val meldeperiode = ObjectMother.meldeperiode(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    versjon = 1,
                    periode = førstePeriode,
                    opprettet = nå(fixedClock),
                    kanFyllesUtFraOgMed = 3.mars(2025).atHour(10),
                )
                val meldekort = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = null,
                    meldeperiode = meldeperiode,
                )
                lagreSak(helper, meldeperiode)
                helper.meldeperiodeRepo.lagre(meldeperiode)
                helper.meldekortPostgresRepo.lagre(meldekort)

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                requireNotNull(resultat).also {
                    it.meldeperiodeId shouldBe meldeperiode.id
                    it.kjedeId shouldBe meldeperiode.kjedeId
                    it.kanFyllesUtFraOgMed shouldBe meldeperiode.kanFyllesUtFraOgMed
                }
            }
        }

        @Test
        fun `returnerer null når meldekort er deaktivert og nyeste meldeperiode har maks 0`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                // Versjon 1 med rett (meldekort deaktivert pga revurdering)
                val meldeperiodeV1 = ObjectMother.meldeperiode(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    versjon = 1,
                    periode = førstePeriode,
                    opprettet = nå(fixedClock),
                )
                lagreSak(helper, meldeperiodeV1)
                helper.meldeperiodeRepo.lagre(meldeperiodeV1)
                val meldekortV1 = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = null,
                    meldeperiode = meldeperiodeV1,
                )
                helper.meldekortPostgresRepo.lagre(meldekortV1)
                helper.meldekortPostgresRepo.deaktiver(meldekortV1.id)

                // Versjon 2 uten rett (opphør)
                val meldeperiodeV2 = meldeperiodeV1.copy(
                    id = MeldeperiodeId.random(),
                    versjon = 2,
                    opprettet = nå(fixedClock).plusHours(1),
                    girRett = førstePeriode.tilDager().associateWith { false },
                    maksAntallDagerForPeriode = 0,
                )
                helper.meldeperiodeRepo.lagre(meldeperiodeV2)

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                resultat shouldBe null
            }
        }

        @Test
        fun `returnerer null når mottatt meldekort på nyeste versjon er deaktivert`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val meldekort = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = nå(fixedClock),
                    periode = førstePeriode,
                )
                lagreMeldekort(helper, meldekort)
                helper.meldekortPostgresRepo.deaktiver(meldekort.id)

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                resultat shouldBe null
            }
        }

        @Test
        fun `returnerer første kjede som mangler innsending sortert på kanFyllesUtFraOgMed`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val andrePeriode = førstePeriode.plus14Dager()

                val førsteMeldekort = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = null,
                    periode = førstePeriode,
                )
                val andreMeldekort = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = null,
                    periode = andrePeriode,
                )
                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                requireNotNull(resultat).kjedeId shouldBe førsteMeldekort.meldeperiode.kjedeId
            }
        }

        @Test
        fun `returnerer første kjede som mangler innsending - ignorerer innsendt kjede`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val andrePeriode = førstePeriode.plus14Dager()

                val innsendtMeldekort = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = nå(fixedClock),
                    periode = førstePeriode,
                )
                val uinnsendtMeldekort = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = null,
                    periode = andrePeriode,
                )
                lagreMeldekort(helper, innsendtMeldekort, uinnsendtMeldekort)

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                requireNotNull(resultat).kjedeId shouldBe uinnsendtMeldekort.meldeperiode.kjedeId
            }
        }

        @Test
        fun `returnerer null for ukjent sakId`() {
            withMigratedDb { helper ->
                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(SakId.random())

                resultat shouldBe null
            }
        }

        @Test
        fun `returnerer første kjede for angitt sak`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val annetSakId = SakId.random()
                lagreMeldekort(
                    helper,
                    ObjectMother.meldekort(
                        sakId = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        mottatt = null,
                        periode = førstePeriode,
                    ),
                    ObjectMother.meldekort(
                        sakId = annetSakId,
                        saksnummer = "annen-sak",
                        fnr = Fnr.fromString("12345678910"),
                        mottatt = null,
                        periode = førstePeriode.plus14Dager(),
                    ),
                )

                val resultat = helper.varselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending(sakId)

                requireNotNull(resultat).sakId shouldBe sakId
            }
        }
    }
}

private fun LocalDate.atHour(time: Int) = this.atTime(time, 0)

private fun lagreSak(helper: TestDataHelper, vararg meldeperioder: Meldeperiode) {
    val meldeperiode = requireNotNull(meldeperioder.firstOrNull()) { "Må ha minst én meldeperiode" }
    if (helper.sakPostgresRepo.hent(meldeperiode.sakId) == null) {
        helper.sakPostgresRepo.lagre(
            ObjectMother.sak(
                id = meldeperiode.sakId,
                saksnummer = meldeperiode.saksnummer,
                fnr = meldeperiode.fnr,
                meldeperioder = meldeperioder.toList(),
            ),
        )
    }
}
