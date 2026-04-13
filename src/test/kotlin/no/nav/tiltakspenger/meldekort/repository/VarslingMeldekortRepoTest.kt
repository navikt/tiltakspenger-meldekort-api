package no.nav.tiltakspenger.meldekort.repository

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.clients.varsler.SendtVarselMetadata
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class VarslingMeldekortRepoTest {

    @Nested
    inner class MarkerVarslet {
        @Test
        fun `kan oppdatere sendtVarselTidspunkt`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val meldekort = ObjectMother.meldekort(mottatt = nå(fixedClock), sendtVarselTidspunkt = null)
                lagreMeldekort(helper, meldekort)

                val tidspunkt = nå(fixedClock).truncatedTo(ChronoUnit.MICROS)
                val metadata = SendtVarselMetadata("\"json-request\"")
                repo.markerVarslet(meldekort.id, tidspunkt, metadata)

                val result = repo.hentForMeldekortId(meldekort.id, meldekort.meldeperiode.fnr)
                result!!.sendtVarselTidspunkt shouldBe tidspunkt
                result.sendtVarsel shouldBe true

                val lagretMetadata = helper.sessionFactory.withSession { session ->
                    session.run(
                        queryOf(
                            "select sendt_varsel_json_request from meldekort_bruker where id = :id",
                            mapOf("id" to meldekort.id.toString()),
                        ).map { row -> row.string("sendt_varsel_json_request") }.asSingle,
                    )
                }
                lagretMetadata shouldBe metadata.jsonRequest
            }
        }
    }

    @Nested
    inner class HentMeldekortDetSkalVarslesFor {
        @Test
        fun `alle matcher kriteriene`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo

                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort1 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    sendtVarselTidspunkt = null,
                    erVarselInaktivert = false,
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    sendtVarselTidspunkt = null,
                    erVarselInaktivert = false,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, meldekort1, meldekort2)

                val result = repo.hentMeldekortDetSkalVarslesFor().sortedBy { it.periode.fraOgMed }

                result.size shouldBe 2
                result[0].id shouldBe meldekort1.id
                result[1].id shouldBe meldekort2.id
            }
        }

        @Test
        fun `henter bare ut relevante meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val meldekortRepo = helper.meldekortPostgresRepo

                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort1 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    sendtVarselTidspunkt = null,
                    erVarselInaktivert = false,
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    sendtVarselTidspunkt = nå(fixedClock),
                    erVarselInaktivert = false,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )
                val meldekort3 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    sendtVarselTidspunkt = nå(fixedClock),
                    erVarselInaktivert = true,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(4),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(4),
                    ),
                )
                val meldekort4 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = nå(fixedClock),
                    sendtVarselTidspunkt = nå(fixedClock),
                    erVarselInaktivert = true,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(6),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(6),
                    ),
                )

                lagreMeldekort(helper, meldekort1, meldekort2, meldekort3, meldekort4)

                val result = meldekortRepo.hentMeldekortDetSkalVarslesFor()

                result.size shouldBe 1
                result[0].id shouldBe meldekort1.id
            }
        }

        @Test
        fun `henter ikke meldekort hvis vi har sendt varsel for forrige meldekort som ikke er mottatt`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val fnr = Fnr.random()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort1 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    sendtVarselTidspunkt = nå(fixedClock),
                    erVarselInaktivert = false,
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    sendtVarselTidspunkt = null,
                    erVarselInaktivert = false,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )

                lagreMeldekort(helper, meldekort1, meldekort2)

                val result = repo.hentMeldekortDetSkalVarslesFor()

                result.size shouldBe 0
            }
        }

        @Test
        fun `henter neste meldekort hvis forrige meldekort er mottatt`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo

                val fnr = Fnr.random()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort1 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = nå(fixedClock),
                    sendtVarselTidspunkt = nå(fixedClock),
                    erVarselInaktivert = true,
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    sendtVarselTidspunkt = null,
                    erVarselInaktivert = false,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )
                val meldekort3 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    sendtVarselTidspunkt = null,
                    erVarselInaktivert = false,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(4),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(4),
                    ),
                )

                lagreMeldekort(helper, meldekort1, meldekort2, meldekort3)

                val result = repo.hentMeldekortDetSkalVarslesFor()

                result.size shouldBe 1
                result[0].id shouldBe meldekort2.id
            }
        }

        @Test
        fun `henter neste meldekort hvis forrige varsel allerede er inaktivert`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo

                val fnr = Fnr.random()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort1 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    sendtVarselTidspunkt = nå(fixedClock),
                    erVarselInaktivert = true,
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    sendtVarselTidspunkt = null,
                    erVarselInaktivert = false,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, meldekort1, meldekort2)

                val result = repo.hentMeldekortDetSkalVarslesFor()

                result.size shouldBe 1
                result[0].id shouldBe meldekort2.id
            }
        }

        @Test
        fun `respekterer limit`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort1 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    sendtVarselTidspunkt = null,
                    erVarselInaktivert = false,
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    sendtVarselTidspunkt = null,
                    erVarselInaktivert = false,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, meldekort1, meldekort2)

                val result = repo.hentMeldekortDetSkalVarslesFor(limit = 1)

                result.size shouldBe 1
            }
        }
    }

    @Nested
    inner class HentMeldekortSomSkalInaktivereVarsel {
        @Test
        fun `alle matcher kriteriene`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort1 = ObjectMother.meldekort(mottatt = nå(fixedClock), sendtVarselTidspunkt = nå(fixedClock))
                val meldekort2 = ObjectMother.meldekort(mottatt = nå(fixedClock), sendtVarselTidspunkt = nå(fixedClock))

                lagreMeldekort(helper, meldekort1, meldekort2)

                val result = repo.henteMeldekortSomSkalInaktivereVarsel()

                result.size shouldBe 2
                result[0].varselId shouldBe meldekort1.varselId
                result[1].varselId shouldBe meldekort2.varselId
            }
        }

        @Test
        fun `henter bare ut relevante meldekort`() {
            withMigratedDb { helper ->
                val meldekortRepo = helper.meldekortPostgresRepo
                val meldekort1 = ObjectMother.meldekort(mottatt = nå(fixedClock), sendtVarselTidspunkt = nå(fixedClock))
                val meldekort2 = ObjectMother.meldekort(mottatt = nå(fixedClock), sendtVarselTidspunkt = nå(fixedClock))
                val meldekort3 = ObjectMother.meldekort(
                    mottatt = nå(fixedClock),
                    sendtVarselTidspunkt = nå(fixedClock),
                    erVarselInaktivert = true,
                )
                val meldekort4 = ObjectMother.meldekort(mottatt = nå(fixedClock), sendtVarselTidspunkt = null)
                val meldekort5 = ObjectMother.meldekort(mottatt = null, sendtVarselTidspunkt = nå(fixedClock))

                lagreMeldekort(helper, meldekort1, meldekort2, meldekort3, meldekort4, meldekort5)

                val result = meldekortRepo.henteMeldekortSomSkalInaktivereVarsel()

                result.size shouldBe 2
                result[0].varselId shouldBe meldekort1.varselId
                result[1].varselId shouldBe meldekort2.varselId
            }
        }

        @Test
        fun `deaktivert meldekort med sendt varsel skal også inaktivere varsel`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(
                    mottatt = null,
                    sendtVarselTidspunkt = nå(fixedClock),
                    erVarselInaktivert = false,
                )

                lagreMeldekort(helper, meldekort)

                // deaktiver med deaktiverVarsel=true slik at varsel_inaktivert forblir false
                repo.deaktiver(meldekort.id, deaktiverVarsel = true)

                val result = repo.henteMeldekortSomSkalInaktivereVarsel()

                result.size shouldBe 1
                result[0].id shouldBe meldekort.id
            }
        }

        @Test
        fun `respekterer limit`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort1 = ObjectMother.meldekort(mottatt = nå(fixedClock), sendtVarselTidspunkt = nå(fixedClock))
                val meldekort2 = ObjectMother.meldekort(mottatt = nå(fixedClock), sendtVarselTidspunkt = nå(fixedClock))

                lagreMeldekort(helper, meldekort1, meldekort2)

                val result = repo.henteMeldekortSomSkalInaktivereVarsel(limit = 1)

                result.size shouldBe 1
            }
        }
    }
}
