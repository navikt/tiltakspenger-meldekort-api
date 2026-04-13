package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.left
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.meldekort.domene.VarselId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class VarslerTest {

    private val sakId = SakId.random()
    private val saksnummer = "SAK-123"
    private val fnr = Fnr.fromString("12345678911")
    private val opprettet = 6.januar(2025).atHour(10)

    /** Mandag kl 10 */
    private val mandagKl10 = 6.januar(2025).atHour(10)

    /** Tirsdag kl 10 */
    private val tirsdagKl10 = 7.januar(2025).atHour(10)

    private fun skalAktiveres(
        skalAktiveresTidspunkt: LocalDateTime = mandagKl10,
        varselId: VarselId = VarselId.random(),
    ) = Varsel.SkalAktiveres(
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        varselId = varselId,
        skalAktiveresTidspunkt = skalAktiveresTidspunkt,
        skalAktiveresBegrunnelse = "test",
        opprettet = opprettet,
        sistEndret = opprettet,
    )

    private fun inaktivert() = Varsel.Inaktivert(
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

    private fun avbrutt() = Varsel.Avbrutt(
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
            Varsler(listOf(inaktivert(), avbrutt(), inaktivert()))
        }

        @Test
        fun `ett ikke-avsluttet varsel pluss ferdige er gyldig`() {
            Varsler(listOf(inaktivert(), skalAktiveres(), avbrutt()))
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
            Varsler(listOf(inaktivert(), avbrutt())).erAlleInaktivertEllerAvbrutt shouldBe true
        }

        @Test
        fun `false når det finnes et aktivt varsel`() {
            Varsler(listOf(inaktivert(), skalAktiveres())).erAlleInaktivertEllerAvbrutt shouldBe false
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
            val varsler = Varsler(listOf(inaktivert(), avbrutt()))
            val resultat = varsler.leggTil(skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10))

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
            val eksisterende = inaktivert()
            val varsler = Varsler(listOf(eksisterende))
            // Nytt varsel med samme dato som eksisterende sin skalAktiveresTidspunkt
            val nyttVarsel = skalAktiveres(skalAktiveresTidspunkt = mandagKl10.withHour(11))

            val resultat = varsler.leggTil(nyttVarsel)

            resultat.shouldBeInstanceOf<arrow.core.Either.Left<Varsler.KanIkkeLeggeTilVarsel.CooldownIkkeUtløpt>>()
        }

        @Test
        fun `kan legge til varsel med annen dato enn eksisterende`() {
            val eksisterende = inaktivert()
            val varsler = Varsler(listOf(eksisterende))
            val nyttVarsel = skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10)

            val resultat = varsler.leggTil(nyttVarsel)

            resultat.isRight() shouldBe true
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
}

private fun java.time.LocalDate.atHour(time: Int): LocalDateTime = this.atTime(time, 0)
