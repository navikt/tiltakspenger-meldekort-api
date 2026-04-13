package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.left
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.meldekort.domene.VarselId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.LocalDateTime

class VarslerTest {

    private val sakId = SakId.random()
    private val saksnummer = "SAK-123"
    private val fnr = Fnr.fromString("12345678911")

    /** Mandag kl 10 */
    private val mandagKl10 = 6.januar(2025).atHour(10)

    /** Tirsdag kl 10 */
    private val tirsdagKl10 = 7.januar(2025).atHour(10)

    private fun nyClock(): Clock = TikkendeKlokke(fixedClockAt(6.januar(2025).atHour(9)))

    private fun skalAktiveres(
        skalAktiveresTidspunkt: LocalDateTime = mandagKl10,
        varselId: VarselId = VarselId.random(),
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
    ): Varsel.SkalAktiveres {
        return Varsel.SkalAktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresBegrunnelse = "test",
            opprettet = opprettet,
            sistEndret = opprettet,
        )
    }

    private fun inaktivert(
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
    ): Varsel.Inaktivert {
        return Varsel.Inaktivert(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = mandagKl10,
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = mandagKl10,
            skalInaktiveresTidspunkt = mandagKl10.plusHours(1),
            skalInaktiveresBegrunnelse = "mottatt",
            inaktiveringstidspunkt = mandagKl10.plusHours(1),
            opprettet = opprettet,
            sistEndret = opprettet,
        )
    }

    private fun avbrutt(
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
    ): Varsel.Avbrutt {
        return Varsel.Avbrutt(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = mandagKl10,
            skalAktiveresBegrunnelse = "test",
            avbruttTidspunkt = opprettet.plusHours(1),
            avbruttBegrunnelse = "opphør",
            opprettet = opprettet,
            sistEndret = opprettet,
        )
    }

    private fun varsler(vararg varsler: Varsel): Varsler = Varsler(varsler.toList().sortedBy { it.opprettet })

    @Nested
    inner class Invarianter {

        @Test
        fun `tom liste er gyldig`() {
            Varsler(emptyList())
        }

        @Test
        fun `en SkalAktiveres er gyldig`() {
            Varsler(listOf(skalAktiveres()))
        }

        @Test
        fun `flere inaktiverte og avbrutte er gyldig`() {
            val clock = nyClock()

            varsler(inaktivert(clock = clock), avbrutt(clock = clock), inaktivert(clock = clock))
        }

        @Test
        fun `ett ikke-avsluttet varsel pluss ferdige er gyldig`() {
            val clock = nyClock()

            varsler(inaktivert(clock = clock), skalAktiveres(clock = clock), avbrutt(clock = clock))
        }

        @Test
        fun `to ikke-avsluttede varsler er ugyldig`() {
            assertThrows<IllegalArgumentException> {
                Varsler(
                    listOf(
                        skalAktiveres(skalAktiveresTidspunkt = mandagKl10),
                        skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10),
                    ),
                )
            }
        }
    }

    @Nested
    inner class ErAlleInaktivertEllerAvbrutt {

        @Test
        fun `true for tom liste`() {
            Varsler(emptyList()).erAlleInaktivertEllerAvbrutt shouldBe true
        }

        @Test
        fun `true når alle er inaktivert eller avbrutt`() {
            val clock = nyClock()

            varsler(inaktivert(clock = clock), avbrutt(clock = clock)).erAlleInaktivertEllerAvbrutt shouldBe true
        }

        @Test
        fun `false når det finnes et aktivt varsel`() {
            val clock = nyClock()

            varsler(inaktivert(clock = clock), skalAktiveres(clock = clock)).erAlleInaktivertEllerAvbrutt shouldBe false
        }
    }

    @Nested
    inner class LeggTilVarsel {

        @Test
        fun `kan legge til varsel på tom liste`() {
            val varsler = Varsler(emptyList())
            val resultat = varsler.leggTil(skalAktiveres())

            resultat.isRight() shouldBe true
            resultat.getOrNull()!! shouldHaveSize 1
        }

        @Test
        fun `kan legge til varsel når alle eksisterende er ferdige`() {
            val clock = nyClock()
            val varsler = varsler(inaktivert(clock = clock), avbrutt(clock = clock))
            val resultat = varsler.leggTil(skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10, clock = clock))

            resultat.isRight() shouldBe true
            resultat.getOrNull()!! shouldHaveSize 3
        }

        @Test
        fun `kan ikke legge til varsel når det finnes et aktivt`() {
            val varsler = Varsler(listOf(skalAktiveres(skalAktiveresTidspunkt = mandagKl10)))
            val resultat = varsler.leggTil(skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10))

            resultat shouldBe Varsler.KanIkkeLeggeTilVarsel.HarAktivtVarsel.left()
        }

        @Test
        fun `kan ikke legge til varsel med samme dato som eksisterende`() {
            val eksisterendeOpprettet = 6.januar(2025).atHour(9)
            val eksisterende = inaktivert(opprettet = eksisterendeOpprettet)
            val varsler = Varsler(listOf(eksisterende))
            // Nytt varsel med samme dato som eksisterende sin skalAktiveresTidspunkt
            val nyttVarsel = skalAktiveres(
                skalAktiveresTidspunkt = mandagKl10.withHour(11),
                opprettet = eksisterendeOpprettet.plusMinutes(1),
            )

            val resultat = varsler.leggTil(nyttVarsel)

            resultat.shouldBeInstanceOf<arrow.core.Either.Left<Varsler.KanIkkeLeggeTilVarsel.CooldownIkkeUtløpt>>()
        }

        @Test
        fun `kan legge til varsel med annen dato enn eksisterende`() {
            val eksisterendeOpprettet = 6.januar(2025).atHour(9)
            val eksisterende = inaktivert(opprettet = eksisterendeOpprettet)
            val varsler = Varsler(listOf(eksisterende))
            val nyttVarsel = skalAktiveres(
                skalAktiveresTidspunkt = tirsdagKl10,
                opprettet = eksisterendeOpprettet.plusMinutes(1),
            )

            val resultat = varsler.leggTil(nyttVarsel)

            resultat.isRight() shouldBe true
        }

        @Test
        fun `Avbrutt varsel samme dag hindrer IKKE nytt varsel - vi skal ikke miste hendelser`() {
            // Regresjonstest: tidligere ble cooldown basert på skalAktiveresTidspunkt,
            // som feilaktig blokkerte nytt varsel når et tidligere varsel ble avbrutt
            // før det rakk å aktiveres (delvis opphør). Da vi aldri sendte varsel til bruker,
            // er det heller ingen grunn til cooldown.
            val eksisterendeOpprettet = 6.januar(2025).atHour(9)
            val avbruttSammeDag = avbrutt(opprettet = eksisterendeOpprettet)
            val varsler = Varsler(listOf(avbruttSammeDag))
            val nyttVarsel = skalAktiveres(
                skalAktiveresTidspunkt = mandagKl10,
                opprettet = eksisterendeOpprettet.plusMinutes(1),
            )

            val resultat = varsler.leggTil(nyttVarsel)

            resultat.isRight() shouldBe true
            resultat.getOrNull()!! shouldHaveSize 2
        }

        @Test
        fun `Inaktivert varsel aktivert på annen dag hindrer IKKE nytt varsel med samme planlagte dato`() {
            // Regresjonstest: Et Inaktivert varsel med skalAktiveresTidspunkt på mandag kan ha
            // blitt faktisk aktivert en annen dag (f.eks. forsinket). Cooldown må baseres på
            // faktisk aktiveringstidspunkt, ikke planlagt.
            val eksisterendeOpprettet = 6.januar(2025).atHour(9)
            val inaktivertMenAktivertTirsdag = Varsel.Inaktivert(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                varselId = VarselId.random(),
                skalAktiveresTidspunkt = mandagKl10,
                skalAktiveresBegrunnelse = "test",
                aktiveringstidspunkt = tirsdagKl10,
                skalInaktiveresTidspunkt = tirsdagKl10.plusHours(1),
                skalInaktiveresBegrunnelse = "mottatt",
                inaktiveringstidspunkt = tirsdagKl10.plusHours(1),
                opprettet = eksisterendeOpprettet,
                sistEndret = eksisterendeOpprettet,
            )
            val varsler = Varsler(listOf(inaktivertMenAktivertTirsdag))
            // Nytt varsel skal planlegges på en annen mandag (samme ukedag men ny uke)
            val nyttVarsel = skalAktiveres(
                skalAktiveresTidspunkt = 13.januar(2025).atHour(10),
                opprettet = eksisterendeOpprettet.plusMinutes(1),
            )

            val resultat = varsler.leggTil(nyttVarsel)

            resultat.isRight() shouldBe true
        }

        @Test
        fun `Inaktivert varsel aktivert samme dag hindrer nytt varsel med samme dato (reell cooldown)`() {
            // Positivtest: hvis det faktisk er sendt et varsel til bruker samme dag,
            // skal cooldown blokkere nytt varsel.
            val eksisterendeOpprettet = 6.januar(2025).atHour(9)
            val eksisterende = inaktivert(opprettet = eksisterendeOpprettet)
            val varsler = Varsler(listOf(eksisterende))
            val nyttVarsel = skalAktiveres(
                skalAktiveresTidspunkt = mandagKl10.withHour(14),
                opprettet = eksisterendeOpprettet.plusMinutes(1),
            )

            val resultat = varsler.leggTil(nyttVarsel)

            resultat.shouldBeInstanceOf<arrow.core.Either.Left<Varsler.KanIkkeLeggeTilVarsel.CooldownIkkeUtløpt>>()
        }
    }

    @Nested
    inner class LeggTilMedParametre {

        @Test
        fun `oppretter SkalAktiveres med riktige verdier`() {
            val clock = fixedClockAt(6.januar(2025))
            val varsler = Varsler(emptyList())

            val resultat = varsler.leggTil(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                skalAktiveresTidspunkt = mandagKl10,
                skalAktiveresBegrunnelse = "ny meldeperiode",
                clock = clock,
            )

            resultat.isRight() shouldBe true
            val oppdatertListe = resultat.getOrNull()!!
            oppdatertListe shouldHaveSize 1
            val varsel = oppdatertListe.single()
            varsel.shouldBeInstanceOf<Varsel.SkalAktiveres>()
            varsel.sakId shouldBe sakId
            varsel.saksnummer shouldBe saksnummer
            varsel.fnr shouldBe fnr
            varsel.skalAktiveresTidspunkt shouldBe mandagKl10
            varsel.skalAktiveresBegrunnelse shouldBe "ny meldeperiode"
        }
    }

    @Nested
    inner class SakIdOgFnrInvarianter {

        @Test
        fun `sakId er null for tom liste`() {
            Varsler(emptyList()).sakId shouldBe null
        }

        @Test
        fun `sakId hentes fra varsler`() {
            Varsler(listOf(skalAktiveres())).sakId shouldBe sakId
        }

        @Test
        fun `fnr hentes fra varsler`() {
            Varsler(listOf(skalAktiveres())).fnr shouldBe fnr
        }
    }

    @Nested
    inner class PlanlagtAktivering {

        @Test
        fun `bruker ønsket tidspunkt når det allerede er gyldig og ingen varsel er sendt samme dag`() {
            Varsler(emptyList()).finnPlanlagtAktiveringstidspunkt(
                ønsketTidspunkt = tirsdagKl10,
                nå = mandagKl10,
            ) shouldBe tirsdagKl10
        }

        @Test
        fun `bruker nå normalisert til åpningstid når ønsket tidspunkt er passert`() {
            Varsler(emptyList()).finnPlanlagtAktiveringstidspunkt(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = 6.januar(2025).atTime(8, 30),
            ) shouldBe 6.januar(2025).atHour(9)
        }

        @Test
        fun `utsetter til neste virkedag når et varsel allerede er aktivert samme dag`() {
            val aktivtVarsel = Varsel.Aktiv(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                varselId = VarselId.random(),
                skalAktiveresTidspunkt = 6.januar(2025).atHour(9),
                skalAktiveresBegrunnelse = "test",
                aktiveringstidspunkt = 6.januar(2025).atTime(9, 5),
                opprettet = 6.januar(2025).atHour(9),
                sistEndret = 6.januar(2025).atTime(9, 5),
            )

            Varsler(listOf(aktivtVarsel)).finnPlanlagtAktiveringstidspunkt(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = 6.januar(2025).atHour(10),
            ) shouldBe 7.januar(2025).atHour(9)
        }

        @Test
        fun `bruker nå når ønsket tidspunkt er passert og nå er innenfor åpningstid`() {
            val nå = 6.januar(2025).atTime(10, 30)

            Varsler(emptyList()).finnPlanlagtAktiveringstidspunkt(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = nå,
            ) shouldBe nå
        }

        @Test
        fun `bruker neste virkedag klokken ni når nå er etter åpningstid`() {
            Varsler(emptyList()).finnPlanlagtAktiveringstidspunkt(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = 3.januar(2025).atHour(18),
            ) shouldBe 6.januar(2025).atHour(9)
        }

        @Test
        fun `bruker neste virkedag klokken ni når nå er i helgen`() {
            Varsler(emptyList()).finnPlanlagtAktiveringstidspunkt(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = 4.januar(2025).atHour(12),
            ) shouldBe 6.januar(2025).atHour(9)
        }
    }
}

private fun java.time.LocalDate.atHour(time: Int): LocalDateTime = this.atTime(time, 0)
