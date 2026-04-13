package no.nav.tiltakspenger.routes.varsel

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode.Companion.kanFyllesUtFraOgMed
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering.MeldekortKorrigertDagDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.routes.korrigering.korrigermeldekort.korrigerMeldekortRequest
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.sendinnmeldekort.sendInnNesteMeldekort
import no.nav.tiltakspenger.routes.withTestApplicationContextAndPostgres
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VarselPostgresEndToEndTest {

    @Test
    fun `mottaSakRequest oppretter og aktiverer varsel når meldekortet kan sendes inn`() {
        val periode = 24.februar(2025) til 9.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val aktivtVarsel = varsler.single()
            aktivtVarsel.shouldBeInstanceOf<Varsel.Aktiv>()
            aktivtVarsel.skalAktiveresTidspunkt shouldBe periode.kanFyllesUtFraOgMed()

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 1
            varslerHendelser.sendteVarsler.single().varselId shouldBe aktivtVarsel.varselId
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest med ingen meldeperioder oppretter ingen varsler`() {
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = emptyList(),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 0

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 0
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest oppretter et planlagt varsel når meldekortet ikke kan sendes inn enda`() {
        val periode = 10 til 23.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val planlagtVarsel = varsler.single()
            planlagtVarsel.shouldBeInstanceOf<Varsel.SkalAktiveres>()
            planlagtVarsel.skalAktiveresTidspunkt shouldBe periode.kanFyllesUtFraOgMed()

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 0
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest med fremtidig meldeperiode avbryter planlagt varsel når ny versjon opphører perioden`() {
        val periode = 10 til 23.mars(2025)
        val opprettet = 1.mars(2025).atHour(10)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val førsteMeldeperiode = meldeperiodeDto(
                periode = periode,
                opprettet = opprettet,
            )
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(førsteMeldeperiode),
            )

            val planlagtVarsel = tac.varselRepo.hentForSakId(sak.id).single().shouldBeInstanceOf<Varsel.SkalAktiveres>().also {
                it.skalAktiveresTidspunkt shouldBe 21.mars(2025).atHour(15)
            }

            mottaSakRequest(
                tac = tac,
                fnr = sak.fnr,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                meldeperioder = listOf(
                    meldeperiodeDto(
                        periode = periode,
                        versjon = 2,
                        opprettet = opprettet.plusDays(1),
                        girRett = periode.tilDager().associateWith { false },
                        antallDagerForPeriode = 0,
                    ),
                ),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val avbruttVarsel = varsler.single().shouldBeInstanceOf<Varsel.Avbrutt>()
            avbruttVarsel.varselId shouldBe planlagtVarsel.varselId

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 0
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest med to fremtidige meldeperioder flytter planlagt varsel når første meldeperiode opphører`() {
        val førstePeriode = 10 til 23.mars(2025)
        val andrePeriode = 24.mars(2025) til 6.april(2025)
        val opprettet = 1.mars(2025).atHour(10)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val førsteMeldeperiode = meldeperiodeDto(
                periode = førstePeriode,
                opprettet = opprettet,
            )
            val andreMeldeperiode = meldeperiodeDto(
                periode = andrePeriode,
                opprettet = opprettet.plusMinutes(1),
            )

            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(førsteMeldeperiode, andreMeldeperiode),
            )

            val opprinneligVarsel = tac.varselRepo.hentForSakId(sak.id).single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
            opprinneligVarsel.skalAktiveresTidspunkt shouldBe førstePeriode.kanFyllesUtFraOgMed()
            opprinneligVarsel.skalAktiveresBegrunnelse.shouldContain(førsteMeldeperiode.kjedeId)

            mottaSakRequest(
                tac = tac,
                fnr = sak.fnr,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                meldeperioder = listOf(
                    meldeperiodeDto(
                        periode = førstePeriode,
                        versjon = 2,
                        opprettet = opprettet.plusDays(1),
                        girRett = førstePeriode.tilDager().associateWith { false },
                        antallDagerForPeriode = 0,
                    ),
                    andreMeldeperiode,
                ),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val oppdatertVarsel = varsler.single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
            oppdatertVarsel.varselId shouldBe opprinneligVarsel.varselId
            oppdatertVarsel.skalAktiveresTidspunkt shouldBe andrePeriode.kanFyllesUtFraOgMed()
            oppdatertVarsel.skalAktiveresBegrunnelse.shouldContain(andreMeldeperiode.kjedeId)
            oppdatertVarsel.skalAktiveresBegrunnelse.shouldContain(andrePeriode.kanFyllesUtFraOgMed().toString())

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 0
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `sendInnNesteMeldekort inaktiverer aktivt varsel etter innsending`() {
        val periode = 24.februar(2025) til 9.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )

            sendInnNesteMeldekort(tac = tac)

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val inaktivertVarsel = varsler.single()
            inaktivertVarsel.shouldBeInstanceOf<Varsel.Inaktivert>()

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 1
            varslerHendelser.inaktiverteVarsler shouldHaveSize 1
            varslerHendelser.sendteVarsler.single().varselId shouldBe inaktivertVarsel.varselId
            varslerHendelser.inaktiverteVarsler.single() shouldBe inaktivertVarsel.varselId
        }
    }

    @Test
    fun `korrigerMeldekortRequest kjører varseljobber og inaktiverer aktivt varsel`() {
        val periode = 24.februar(2025) til 9.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )

            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac, runJobs = false)!!

            tac.varselRepo.hentForSakId(sak.id).single().shouldBeInstanceOf<Varsel.Aktiv>()

            val korrigerteDager = innsendtMeldekort.dager.mapIndexed { index, dag ->
                MeldekortKorrigertDagDTO(
                    dato = dag.dag,
                    status = if (index == 0) {
                        MeldekortDagStatus.FRAVÆR_SYK
                    } else {
                        dag.status
                    },
                )
            }

            korrigerMeldekortRequest(
                tac = tac,
                meldekortId = innsendtMeldekort.id.toString(),
                requestBody = serialize(korrigerteDager),
                locale = "nb",
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val inaktivertVarsel = varsler.single()
            inaktivertVarsel.shouldBeInstanceOf<Varsel.Inaktivert>()

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 1
            varslerHendelser.inaktiverteVarsler shouldHaveSize 1
            varslerHendelser.inaktiverteVarsler.single() shouldBe inaktivertVarsel.varselId
        }
    }
}

private fun LocalDate.atHour(time: Int): LocalDateTime = this.atTime(time, 0)
