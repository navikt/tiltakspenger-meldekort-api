package no.nav.tiltakspenger.varsler

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClientFake
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering.MeldekortKorrigertDagDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.routes.jobber.KjørJobberForTester
import no.nav.tiltakspenger.routes.korrigering.korrigermeldekort.korrigerMeldekortRequest
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.sendinnmeldekort.sendInnNesteMeldekort
import no.nav.tiltakspenger.routes.withTestApplicationContext
import no.nav.tiltakspenger.routes.withTestApplicationContextAndPostgres
import org.junit.jupiter.api.Test

class VarslerIntegrationTest {
    private val fnr = Fnr.fromString(FAKE_FNR)
    private val førstePeriode = 6 til 19.januar(2025)
    private val andrePeriode = Periode(
        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
    )

    @Test
    fun `sender varsel for første tilgjengelige meldekort bare en gang`() = runTest {
        withBeggeTestApplicationContext { tac ->
            val clock = tac.clock
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(
                    meldeperiodeDto(periode = førstePeriode, opprettet = nå(clock)),
                    meldeperiodeDto(periode = andrePeriode, opprettet = nå(clock)),
                ),
            )
            val førsteMeldekort = tac.hentAlleKlarTilUtfylling().first()

            KjørJobberForTester.kjørVarsler(tac)
            tac.assertVarselhendelser(
                sendteMeldekort = listOf(førsteMeldekort),
                inaktiverteMeldekort = emptyList(),
            )
            tac.assertLagretMeldekortMedVarseloppdateringer(
                originaltMeldekort = førsteMeldekort,
                sendtVarsel = true,
            )

            KjørJobberForTester.kjørVarsler(tac)

            tac.assertVarselhendelser(
                sendteMeldekort = listOf(førsteMeldekort),
                inaktiverteMeldekort = emptyList(),
            )
        }
    }

    @Test
    fun `sender neste varsel og inaktiverer forrige i samme varselsyklus etter innsending`() = runTest {
        withBeggeTestApplicationContext { tac ->
            val clock = tac.clock
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(
                    meldeperiodeDto(periode = førstePeriode, opprettet = nå(clock)),
                    meldeperiodeDto(periode = andrePeriode, opprettet = nå(clock)),
                ),
            )
            val (førsteMeldekort, andreMeldekort) = tac.hentAlleKlarTilUtfylling()

            KjørJobberForTester.kjørVarsler(tac)
            tac.assertVarselhendelser(
                sendteMeldekort = listOf(førsteMeldekort),
                inaktiverteMeldekort = emptyList(),
            )

            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac)!!

            KjørJobberForTester.kjørVarsler(tac)

            tac.assertVarselhendelser(
                sendteMeldekort = listOf(
                    førsteMeldekort,
                    andreMeldekort,
                ),
                inaktiverteMeldekort = listOf(førsteMeldekort),
            )
            tac.assertLagretMeldekortMedVarseloppdateringer(
                originaltMeldekort = innsendtMeldekort,
                sendtVarsel = true,
                erVarselInaktivert = true,
            )
            tac.assertLagretMeldekortMedVarseloppdateringer(
                originaltMeldekort = andreMeldekort,
                sendtVarsel = true,
            )

            KjørJobberForTester.kjørVarsler(tac)

            tac.assertVarselhendelser(
                sendteMeldekort = listOf(
                    førsteMeldekort,
                    andreMeldekort,
                ),
                inaktiverteMeldekort = listOf(førsteMeldekort),
            )
            tac.assertLagretMeldekortMedVarseloppdateringer(
                originaltMeldekort = andreMeldekort,
                sendtVarsel = true,
            )
        }
    }

    @Test
    fun `revurdering av ikke innsendt meldekort gjenbruker eksisterende varsel uten nye jobbsideeffekter`() = runTest {
        withBeggeTestApplicationContext { tac ->
            val clock = tac.clock
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = førstePeriode, opprettet = nå(clock))),
            )

            KjørJobberForTester.kjørVarsler(tac)
            val meldekortEtterVarsel = tac.hentMeldekortForKjede(førstePeriode).single()
            val baseline = tac.tmsVarselClient.snapshotVarselhendelser()

            mottaSakRequest(
                tac = tac,
                fnr = sak.fnr,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                meldeperioder = listOf(
                    meldeperiodeDto(
                        periode = førstePeriode,
                        versjon = 2,
                        opprettet = nå(clock),
                    ),
                ),
            )

            val meldekortForKjede = tac.hentMeldekortForKjede(førstePeriode)
            meldekortForKjede.size shouldBe 2

            val gammeltMeldekort = meldekortForKjede[0]
            val nyttMeldekort = meldekortForKjede[1]

            gammeltMeldekort.deaktivert shouldNotBe null
            gammeltMeldekort.erVarselInaktivert shouldBe true
            nyttMeldekort.deaktivert shouldBe null
            nyttMeldekort.varselId shouldBe meldekortEtterVarsel.varselId
            nyttMeldekort.sendtVarsel shouldBe true
            nyttMeldekort.sendtVarselTidspunkt shouldBe meldekortEtterVarsel.sendtVarselTidspunkt

            KjørJobberForTester.kjørVarsler(tac)

            tac.tmsVarselClient.snapshotVarselhendelser() shouldBe baseline
        }
    }

    @Test
    fun `revurdering som fjerner all rett inaktiverer eksisterende varsel uten å sende nytt`() = runTest {
        withBeggeTestApplicationContext { tac ->
            val clock = tac.clock
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = førstePeriode, opprettet = nå(clock))),
            )

            KjørJobberForTester.kjørVarsler(tac)
            val varsletMeldekort = tac.hentMeldekortForKjede(førstePeriode).single()

            mottaSakRequest(
                tac = tac,
                fnr = sak.fnr,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                meldeperioder = listOf(
                    meldeperiodeDto(
                        periode = førstePeriode,
                        versjon = 2,
                        girRett = førstePeriode.tilDager().associateWith { false },
                        antallDagerForPeriode = 0,
                        opprettet = nå(clock),
                    ),
                ),
            )

            val meldekortForKjede = tac.hentMeldekortForKjede(førstePeriode)
            meldekortForKjede.size shouldBe 1
            meldekortForKjede.single().also { deaktivertMeldekort ->
                deaktivertMeldekort.deaktivert shouldNotBe null
                deaktivertMeldekort.erVarselInaktivert shouldBe false
            }

            KjørJobberForTester.kjørVarsler(tac)

            tac.assertVarselhendelser(
                sendteMeldekort = listOf(varsletMeldekort),
                inaktiverteMeldekort = listOf(varsletMeldekort),
            )
            tac.assertLagretMeldekortMedVarseloppdateringer(
                originaltMeldekort = varsletMeldekort,
                sendtVarsel = true,
                erVarselInaktivert = true,
                forventerDeaktivert = true,
            )
        }
    }

    @Test
    fun `korrigering fører ikke til nye varsler eller nye inaktiveringer`() = runTest {
        withBeggeTestApplicationContext { tac ->
            val clock = tac.clock
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = førstePeriode, opprettet = nå(clock))),
            )

            KjørJobberForTester.kjørVarsler(tac)
            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac)!!
            KjørJobberForTester.kjørVarsler(tac)
            val baseline = tac.tmsVarselClient.snapshotVarselhendelser()

            korrigerMeldekortRequest(
                meldekortId = innsendtMeldekort.id.toString(),
                requestBody = førstePeriode.tilKorrigerteDagerJson(),
                locale = "nb",
            ) shouldNotBe null

            KjørJobberForTester.kjørVarsler(tac)

            tac.tmsVarselClient.snapshotVarselhendelser() shouldBe baseline
            tac.meldekortRepo.hentSisteUtfylteMeldekort(fnr)!!.also { korrigertMeldekort ->
                korrigertMeldekort.korrigering shouldBe true
                korrigertMeldekort.sendtVarsel shouldBe false
                korrigertMeldekort.erVarselInaktivert shouldBe false
            }
        }
    }

    private fun withBeggeTestApplicationContext(
        clockFactory: () -> TikkendeKlokke = ::tikkendeKlokke1mars2025,
        testBlock: suspend ApplicationTestBuilder.(TestApplicationContext) -> Unit,
    ) {
        withTestApplicationContext(clock = clockFactory()) { tac ->
            testBlock(tac)
        }
        withTestApplicationContextAndPostgres(
            runIsolated = true,
            clock = clockFactory(),
        ) { tac ->
            testBlock(tac)
        }
    }

    private fun TestApplicationContext.hentAlleKlarTilUtfylling(): List<Meldekort> {
        return meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr)
    }

    private fun TestApplicationContext.hentMeldekortForKjede(periode: Periode): List<Meldekort> {
        return meldekortRepo.hentMeldekortForKjedeId(
            MeldeperiodeKjedeId.fraPeriode(periode),
            fnr,
        ).toList()
    }

    private fun TestApplicationContext.assertVarselhendelser(
        sendteMeldekort: List<Meldekort>,
        inaktiverteMeldekort: List<Meldekort>,
    ) {
        tmsVarselClient.snapshotVarselhendelser() shouldBe TmsVarselClientFake.Varselhendelser(
            sendteVarsler = sendteMeldekort.map { it.tilSendtVarsel() },
            inaktiverteVarsler = inaktiverteMeldekort.map { it.tilInaktivertVarsel() },
        )
    }

    private fun TestApplicationContext.assertLagretMeldekortMedVarseloppdateringer(
        originaltMeldekort: Meldekort,
        varselId: VarselId = originaltMeldekort.varselId,
        sendtVarsel: Boolean = originaltMeldekort.sendtVarsel,
        erVarselInaktivert: Boolean = originaltMeldekort.erVarselInaktivert,
        forventerDeaktivert: Boolean = originaltMeldekort.deaktivert != null,
    ) {
        val lagretMeldekort = meldekortRepo.hentForMeldekortId(originaltMeldekort.id, fnr)!!

        lagretMeldekort shouldBe originaltMeldekort.copy(
            deaktivert = when {
                forventerDeaktivert ->
                    originaltMeldekort.deaktivert
                        ?: lagretMeldekort.deaktivert.also { it shouldNotBe null }

                else -> null
            },
            varselId = varselId,
            sendtVarsel = sendtVarsel,
            sendtVarselTidspunkt = when {
                sendtVarsel ->
                    originaltMeldekort.sendtVarselTidspunkt
                        ?: lagretMeldekort.sendtVarselTidspunkt.also { it shouldNotBe null }

                else -> null
            },
            erVarselInaktivert = erVarselInaktivert,
        )
    }

    private fun Meldekort.tilSendtVarsel(): TmsVarselClientFake.SendtVarsel {
        return TmsVarselClientFake.SendtVarsel(
            meldekortId = id,
            varselId = varselId,
        )
    }

    private fun Meldekort.tilInaktivertVarsel(): VarselId {
        return varselId
    }

    private fun Periode.tilKorrigerteDagerJson(): String {
        return serialize(
            this.tilDager().map { dag ->
                MeldekortKorrigertDagDTO(
                    dato = dag,
                    status = if (dag.dayOfWeek.value <= 5) {
                        MeldekortDagStatus.FRAVÆR_SYK
                    } else {
                        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                    },
                )
            },
        )
    }
}
