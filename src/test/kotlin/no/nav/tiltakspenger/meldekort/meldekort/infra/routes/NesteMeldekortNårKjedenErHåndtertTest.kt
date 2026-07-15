package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.lagreMeldekortvedtak
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

/**
 * Verifiserer at "neste meldekort til utfylling" (det som avgjør hva bruker mangler innsending på) tar høyde for at en kjede kan være håndtert utenfor den digitale flyten:
 *  - via et saksbehandlervedtak (papirmeldekort) -> [task 2]
 *  - via at bruker allerede har sendt inn et digitalt meldekort -> [task 3]
 *
 * I begge tilfeller skal en SENERE søknadsbehandling som er håndtert ikke "blokkere", og neste meldekort til utfylling skal være den første meldeperioden fra den TIDLIGERE (andre) søknadsbehandlingen som faktisk mangler innsending.
 */
class NesteMeldekortNårKjedenErHåndtertTest {

    // Andre (tidligere) søknadsbehandling – disse mangler innsending.
    private val tidligerePeriode1 = Periode(6.januar(2025), 19.januar(2025))
    private val tidligerePeriode2 = Periode(20.januar(2025), 2.februar(2025))

    // Første søknadsbehandling – en senere periode som blir håndtert (papir / allerede innsendt).
    private val senerePeriode = Periode(3.februar(2025), 16.februar(2025))

    @Test
    fun `neste meldekort hopper over kjede med saksbehandlervedtak og blir første meldeperiode fra andre søknadsbehandling`() =
        runTest {
            withTestApplicationContextAndPostgres(
                clock = TikkendeKlokke(fixedClockAt(1.mars(2025).atTime(10, 0))),
            ) { tac ->
                // Egen person/sak slik at testen kan kjøre side-om-side med andre saker i delt test-db.
                val fnr = tac.nesteFnr()
                // 1. Første søknadsbehandling: senere periode -> det opprettes et tomt meldekort for kjeden.
                val senereMeldeperiode = ObjectMother.meldeperiodeDto(periode = senerePeriode, opprettet = nå(tac.clock))
                val sak = mottaSakRequest(
                    tac = tac,
                    fnr = fnr,
                    meldeperioder = listOf(senereMeldeperiode),
                    runJobs = false,
                )

                // Saksbehandlervedtak (papirmeldekort) for den senere kjeden.
                val senereMeldekort = tac.meldekortRepo
                    .hentMeldekortForKjedeId(MeldeperiodeKjedeId.fraPeriode(senerePeriode), sak.fnr)
                    .first()
                tac.lagreMeldekortvedtak(
                    ObjectMother.meldekortvedtak(meldekort = senereMeldekort, opprettet = nå(tac.clock)),
                )

                // 2. Andre søknadsbehandling, som dekker en tidligere periode (uten vedtak).
                val tidligereMeldeperiode1 = ObjectMother.meldeperiodeDto(periode = tidligerePeriode1, opprettet = nå(tac.clock))
                val tidligereMeldeperiode2 = ObjectMother.meldeperiodeDto(periode = tidligerePeriode2, opprettet = nå(tac.clock))
                mottaSakRequest(
                    tac = tac,
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = sak.fnr,
                    // Saken sendes alltid med full tilstand av meldeperioder.
                    meldeperioder = listOf(tidligereMeldeperiode1, tidligereMeldeperiode2, senereMeldeperiode),
                    runJobs = false,
                )

                // Neste meldekort til utfylling = første meldeperiode fra andre søknadsbehandling.
                val neste = tac.meldekortRepo.hentNesteMeldekortTilUtfylling(sak.fnr)
                neste!!.meldeperiode.id.toString() shouldBe tidligereMeldeperiode1.id

                // Den senere kjeden (med vedtak) skal ikke regnes som "mangler innsending".
                tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(sak.fnr)
                    .map { it.meldeperiode.id.toString() } shouldBe listOf(tidligereMeldeperiode1.id, tidligereMeldeperiode2.id)
            }
        }

    @Test
    fun `neste meldekort hopper over kjede der bruker allerede har sendt inn digitalt meldekort`() =
        runTest {
            withTestApplicationContextAndPostgres(
                clock = TikkendeKlokke(fixedClockAt(1.mars(2025).atTime(10, 0))),
            ) { tac ->
                // Egen person/sak slik at testen kan kjøre side-om-side med andre saker i delt test-db.
                val fnr = tac.nesteFnr()
                // 1. Første søknadsbehandling: senere periode -> tomt meldekort for kjeden.
                val senereMeldeperiode = ObjectMother.meldeperiodeDto(periode = senerePeriode, opprettet = nå(tac.clock))
                val sak = mottaSakRequest(
                    tac = tac,
                    fnr = fnr,
                    meldeperioder = listOf(senereMeldeperiode),
                    runJobs = false,
                )

                // Bruker sender inn et digitalt meldekort for den senere kjeden.
                // Henter det tomme meldekortet som ble opprettet for kjeden, og fyller det ut som om bruker gjorde det.
                val senereMeldekort = tac.meldekortRepo
                    .hentMeldekortForKjedeId(MeldeperiodeKjedeId.fraPeriode(senerePeriode), sak.fnr)
                    .first()
                val kommando = ObjectMother.lagreMeldekortFraBrukerKommando(
                    meldeperiode = senereMeldekort.meldeperiode,
                    meldekortId = senereMeldekort.id,
                )
                tac.meldekortRepo.lagre(
                    senereMeldekort.fyllUtMeldekortFraBruker(
                        sisteMeldeperiode = senereMeldekort.meldeperiode,
                        clock = tac.clock,
                        brukerutfylteDager = kommando.dager,
                        korrigering = false,
                        locale = null,
                    ),
                )

                // 2. Andre søknadsbehandling, som dekker en tidligere periode (ikke innsendt).
                val tidligereMeldeperiode1 = ObjectMother.meldeperiodeDto(periode = tidligerePeriode1, opprettet = nå(tac.clock))
                val tidligereMeldeperiode2 = ObjectMother.meldeperiodeDto(periode = tidligerePeriode2, opprettet = nå(tac.clock))
                mottaSakRequest(
                    tac = tac,
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = sak.fnr,
                    meldeperioder = listOf(tidligereMeldeperiode1, tidligereMeldeperiode2, senereMeldeperiode),
                    runJobs = false,
                )

                // Neste meldekort til utfylling = første meldeperiode fra andre søknadsbehandling.
                val neste = tac.meldekortRepo.hentNesteMeldekortTilUtfylling(sak.fnr)
                neste!!.meldeperiode.id.toString() shouldBe tidligereMeldeperiode1.id

                // Den senere kjeden (allerede innsendt) skal ikke regnes som "mangler innsending".
                tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(sak.fnr)
                    .map { it.meldeperiode.id.toString() } shouldBe listOf(tidligereMeldeperiode1.id, tidligereMeldeperiode2.id)
            }
        }
}
