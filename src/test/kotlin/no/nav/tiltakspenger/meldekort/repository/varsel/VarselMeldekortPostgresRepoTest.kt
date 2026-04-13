package no.nav.tiltakspenger.meldekort.repository.varsel

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat shouldHaveSize 1
                resultat.single().also {
                    it.sakId shouldBe sakId
                    it.kjedeId shouldBe meldekort.meldeperiode.kjedeId
                    it.nyesteVersjon shouldBe 1
                    it.kanFyllesUtFraOgMed shouldBe meldekort.meldeperiode.kanFyllesUtFraOgMed
                }
            }
        }

        @Test
        fun `returnerer tom liste når meldekort er innsendt for nyeste versjon`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val meldekort = ObjectMother.meldekort(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    mottatt = nå(fixedClock),
                    periode = førstePeriode,
                )
                lagreMeldekort(helper, meldekort)

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat.shouldBeEmpty()
            }
        }

        @Test
        fun `returnerer kjede når revurdering (versjon 2) mangler innsending selv om versjon 1 var innsendt`() {
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

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat shouldHaveSize 1
                resultat.single().also {
                    it.kjedeId shouldBe meldeperiodeV1.kjedeId
                    it.nyesteVersjon shouldBe 2
                }
            }
        }

        @Test
        fun `returnerer tom liste når revurdering (versjon 2) har innsendt meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val meldeperiodeV1 = ObjectMother.meldeperiode(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    versjon = 1,
                    periode = førstePeriode,
                    opprettet = nå(fixedClock),
                )
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

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat.shouldBeEmpty()
            }
        }

        @Test
        fun `returnerer tom liste når nyeste meldeperiode har maks_antall_dager_for_periode lik 0`() {
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
                helper.meldeperiodeRepo.lagre(meldeperiode)

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat.shouldBeEmpty()
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
                helper.meldeperiodeRepo.lagre(meldeperiode)
                helper.sakPostgresRepo.lagre(
                    ObjectMother.sak(
                        id = sakId,
                        saksnummer = saksnummer,
                        fnr = fnr,
                        meldeperioder = listOf(meldeperiode),
                    ),
                )

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat shouldHaveSize 1
                resultat.single().also {
                    it.meldeperiodeId shouldBe meldeperiode.id
                    it.kjedeId shouldBe meldeperiode.kjedeId
                    it.nyesteVersjon shouldBe meldeperiode.versjon
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
                helper.meldeperiodeRepo.lagre(meldeperiode)
                helper.meldekortPostgresRepo.lagre(meldekort)

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat shouldHaveSize 1
                resultat.single().also {
                    it.meldeperiodeId shouldBe meldeperiode.id
                    it.kjedeId shouldBe meldeperiode.kjedeId
                    it.kanFyllesUtFraOgMed shouldBe meldeperiode.kanFyllesUtFraOgMed
                }
            }
        }

        @Test
        fun `returnerer tom liste når meldekort er deaktivert og nyeste meldeperiode har maks 0`() {
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

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat.shouldBeEmpty()
            }
        }

        @Test
        fun `returnerer kjede når innsendt meldekort på nyeste versjon er deaktivert`() {
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

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat shouldHaveSize 1
                resultat.single().kjedeId shouldBe meldekort.meldeperiode.kjedeId
            }
        }

        @Test
        fun `returnerer flere kjeder som mangler innsending sortert på kanFyllesUtFraOgMed`() {
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

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat shouldHaveSize 2
                resultat[0].kjedeId shouldBe førsteMeldekort.meldeperiode.kjedeId
                resultat[1].kjedeId shouldBe andreMeldekort.meldeperiode.kjedeId
            }
        }

        @Test
        fun `returnerer bare kjeder som mangler innsending - ignorerer innsendt kjede`() {
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

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat shouldHaveSize 1
                resultat.single().kjedeId shouldBe uinnsendtMeldekort.meldeperiode.kjedeId
            }
        }

        @Test
        fun `returnerer tom liste for ukjent sakId`() {
            withMigratedDb { helper ->
                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(SakId.random())

                resultat.shouldBeEmpty()
            }
        }

        @Test
        fun `returnerer bare kjeder for angitt sak`() {
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

                val resultat = helper.varselMeldekortPostgresRepo.hentKjederSomManglerInnsending(sakId)

                resultat shouldHaveSize 1
                resultat.single().sakId shouldBe sakId
            }
        }
    }
}

private fun LocalDate.atHour(time: Int) = this.atTime(time, 0)
