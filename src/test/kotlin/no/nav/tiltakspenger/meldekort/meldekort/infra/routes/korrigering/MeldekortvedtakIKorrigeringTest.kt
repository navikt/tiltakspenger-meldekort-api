package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.lagreMeldekortvedtak
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDag
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekort.VedtattMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortDagStatusDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortMedSisteMeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.erInnsendt
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.hentAlleInnsendteMeldekortRequest
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.sendInnNesteMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.shouldBeAlleMeldekortJson
import no.nav.tiltakspenger.meldekort.meldekort.infra.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.tilMeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

/**
 * Verifiserer at bruker-flaten («allerede utfylt» + korrigering) tar høyde for meldekortvedtak
 * (papirmeldekort / saksbehandler-behandlede meldeperioder):
 *  - en papir-only kjede (kun vedtak, ingen digital innsending — brukersMeldekortId == null) skal
 *    vises i innsendte-lista, kunne korrigeres og preutfylles fra vedtaket.
 *  - en kjede der bruker har sendt inn digitalt og saksbehandler senere overstyrer en av brukers
 *    verdier (vedtak basert på brukers innsending — brukersMeldekortId satt) skal fortsatt
 *    preutfylle korrigeringen fra brukers eget innsendte meldekort, ikke fra vedtaket.
 */
class MeldekortvedtakIKorrigeringTest {

    private val periode = Periode(6.januar(2025), 19.januar(2025))

    @Test
    fun `papir-only kjede vises i innsendte, kan korrigeres og preutfylles fra vedtaket`() = runTest {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(1.mars(2025).atTime(10, 0))),
        ) { tac ->
            val fnr = tac.nesteFnr()
            val meldeperiode = ObjectMother.meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))
            val sak = mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(meldeperiode),
                runJobs = false,
            )

            // Kjeden har kun et åpent (uinnsendt) meldekort – ingen digital innsending.
            val åpentMeldekort = tac.meldekortRepo
                .hentMeldekortForKjedeId(MeldeperiodeKjedeId.fraPeriode(periode), sak.fnr)
                .first()
            val vedtak = ObjectMother.meldekortvedtak(meldekort = åpentMeldekort, opprettet = nå(tac.clock))
            tac.lagreMeldekortvedtak(vedtak)

            // 1. Vises i innsendte-lista som en innsendt tilstand med vedtakets tilstand og id = åpent meldekort.
            val forventetRegistrert = VedtattMeldekort(
                id = åpentMeldekort.id,
                meldeperiode = åpentMeldekort.meldeperiode,
                dager = vedtak.meldeperiodebehandlinger.single().dager.map {
                    MeldekortDag(dag = it.dato, status = it.status)
                },
                opprettet = vedtak.opprettet,
            )
            val json = hentAlleInnsendteMeldekortRequest(fnr = sak.fnr.verdi)!!
            json.shouldBeAlleMeldekortJson(
                meldekortMedSisteMeldeperiode = listOf(
                    MeldekortMedSisteMeldeperiodeDTO(
                        meldekort = forventetRegistrert.tilMeldekortTilBrukerDTO(),
                        sisteMeldeperiode = sak.meldeperioder.last().tilMeldeperiodeDTO(),
                    ),
                ),
            )

            // 2. Kan korrigeres.
            kanMeldekortKorrigeresRequest(
                fnr = sak.fnr.verdi,
                meldekortId = åpentMeldekort.id.toString(),
            )!!.shouldBe(kanKorrigeres = true)

            // 3. Korrigering preutfylles fra vedtaket.
            val korrigering = hentKorrigeringForIdRequest(
                fnr = sak.fnr.verdi,
                meldekortId = åpentMeldekort.id.toString(),
            )!!
            korrigering.tilUtfylling.mottattTidspunktSisteMeldekort shouldBe vedtak.opprettet
            korrigering.forrigeMeldekort.innsendt shouldBe vedtak.opprettet
            korrigering.forrigeMeldekort.status shouldBe MeldekortStatusDTO.INNSENDT
            korrigering.tilUtfylling.dager.forEach { dag ->
                val forventet = if (meldeperiode.girRett[dag.dag] == true) {
                    MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
                } else {
                    MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
                }
                dag.status shouldBe forventet
            }

            // 4. Korrigering av en papir-only kjede lager ALLTID et nytt meldekort (korrigering),
            //    og deaktiverer det åpne placeholder-meldekortet. Placeholderen fylles aldri in-place.
            val korrigerteDager = periode.tilDager().map { dato ->
                MeldekortKorrigertDagDTO(
                    dato = dato,
                    status = if (dato.dayOfWeek.value <= 5) {
                        MeldekortDagStatus.FRAVÆR_SYK
                    } else {
                        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                    },
                )
            }
            val korrigert = korrigerMeldekortRequest(
                tac = tac,
                meldekortId = åpentMeldekort.id.toString(),
                requestDto = korrigerteDager,
                locale = "nb",
                fnr = sak.fnr.verdi,
                runJobs = false,
            )!!
            korrigert.erInnsendt()
            korrigert.dager.size shouldBe 14
            // Nytt meldekort med ny id — ikke placeholderens id.
            korrigert.id shouldNotBe åpentMeldekort.id.toString()

            // Det nye meldekortet er en korrigering og er mottatt; placeholderen er deaktivert.
            val nyttMeldekort = tac.meldekortRepo.hentForMeldekortId(
                MeldekortId.fromString(korrigert.id),
                sak.fnr,
            )!!
            nyttMeldekort.korrigering shouldBe true
            nyttMeldekort.mottatt shouldNotBe null

            val placeholderEtterKorrigering = tac.meldekortRepo.hentForMeldekortId(åpentMeldekort.id, sak.fnr)!!
            placeholderEtterKorrigering.deaktivert shouldNotBe null

            // Innsendte-lista viser nå det korrigerte (digitale) meldekortet — ikke en stale VedtattMeldekort,
            // og ingen dobbel-listing for kjeden.
            val innsendteEtterKorrigering = hentAlleInnsendteMeldekortRequest(fnr = sak.fnr.verdi)!!
            innsendteEtterKorrigering.shouldBeAlleMeldekortJson(
                forrigeMeldekort = nyttMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                meldekortMedSisteMeldeperiode = listOf(
                    MeldekortMedSisteMeldeperiodeDTO(
                        meldekort = nyttMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                        sisteMeldeperiode = sak.meldeperioder.last().tilMeldeperiodeDTO(),
                    ),
                ),
            )
        }
    }

    @Test
    fun `digital innsending der saksbehandler overstyrer brukers verdier preutfylles fra brukers eget meldekort`() = runTest {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(1.mars(2025).atTime(10, 0))),
        ) { tac ->
            val fnr = tac.nesteFnr()
            val meldeperiode = ObjectMother.meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))
            val sak = mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(meldeperiode),
                runJobs = false,
            )

            // Bruker sender inn digitalt (DELTATT_UTEN_LØNN på dager som gir rett).
            val (_, digitaltMeldekort) = sendInnNesteMeldekort(tac = tac, fnr = sak.fnr.verdi, runJobs = false)!!

            // Saksbehandler overstyrer en av brukers verdier. Vedtaket er basert på brukers innsending
            // (brukersMeldekortId = brukers meldekort) og har avvikende dager (FRAVÆR_SYK).
            val vedtak = ObjectMother.meldekortvedtak(
                sakId = digitaltMeldekort.sakId,
                meldeperiodeId = digitaltMeldekort.meldeperiode.id,
                meldeperiodeKjedeId = digitaltMeldekort.meldeperiode.kjedeId,
                periode = digitaltMeldekort.periode,
                opprettet = nå(tac.clock),
                brukersMeldekortId = digitaltMeldekort.id,
                dager = digitaltMeldekort.periode.tilDager().map { dato ->
                    no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldeperiodebehandlingDag(
                        dato = dato,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                        reduksjon = no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon.INGEN_REDUKSJON,
                        beløp = 0,
                        beløpBarnetillegg = 0,
                    )
                },
            )
            tac.lagreMeldekortvedtak(vedtak)

            kanMeldekortKorrigeresRequest(
                fnr = sak.fnr.verdi,
                meldekortId = digitaltMeldekort.id.toString(),
            )!!.shouldBe(kanKorrigeres = true)

            // Korrigeringen tar utgangspunkt i brukers eget innsendte meldekort, ikke i vedtaket.
            val korrigering = hentKorrigeringForIdRequest(
                fnr = sak.fnr.verdi,
                meldekortId = digitaltMeldekort.id.toString(),
            )!!
            korrigering.forrigeMeldekort.id shouldBe digitaltMeldekort.id.toString()
            korrigering.tilUtfylling.mottattTidspunktSisteMeldekort shouldBe digitaltMeldekort.mottatt
            korrigering.forrigeMeldekort.innsendt shouldBe digitaltMeldekort.mottatt

            // Dager som gir rett preutfylles fra brukers innsending (DELTATT_UTEN_LØNN), ikke fra
            // vedtakets overstyrte verdier (FRAVÆR_SYK).
            korrigering.tilUtfylling.dager.forEach { dag ->
                val forventet = if (meldeperiode.girRett[dag.dag] == true) {
                    MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
                } else {
                    MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
                }
                dag.status shouldBe forventet
            }

            // Korrigeringen lager et nytt meldekort (digital innsending var allerede mottatt).
            val korrigerteDager = periode.tilDager().map { dato ->
                MeldekortKorrigertDagDTO(
                    dato = dato,
                    status = if (dato.dayOfWeek.value <= 5) {
                        MeldekortDagStatus.FRAVÆR_SYK
                    } else {
                        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                    },
                )
            }
            val korrigert = korrigerMeldekortRequest(
                tac = tac,
                meldekortId = digitaltMeldekort.id.toString(),
                requestDto = korrigerteDager,
                locale = "nb",
                fnr = sak.fnr.verdi,
                runJobs = false,
            )!!
            korrigert.erInnsendt()
            korrigert.id shouldNotBe digitaltMeldekort.id.toString()
            korrigert.dager.size shouldBe 14
        }
    }
}
