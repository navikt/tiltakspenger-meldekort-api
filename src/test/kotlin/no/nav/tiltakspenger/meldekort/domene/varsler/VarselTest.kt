package no.nav.tiltakspenger.meldekort.domene.varsler

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.meldekort.domene.VarselId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
class VarselTest {

    private val sakId = SakId.random()
    private val saksnummer = "SAK-123"
    private val fnr = Fnr.fromString("12345678911")
    private val varselId = VarselId.random()
    private val opprettet = 6.januar(2025).atHour(10)

    /** Mandag kl 10 */
    private val gyldigAktiveringsTidspunkt = 6.januar(2025).atHour(10)

    private fun skalAktiveres(
        skalAktiveresTidspunkt: LocalDateTime = gyldigAktiveringsTidspunkt,
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

    @Nested
    inner class SkalAktiveresTest {

        @Test
        fun `kan opprettes med gyldig tidspunkt på ukedag innenfor kontortid`() {
            val varsel = skalAktiveres()
            varsel.type shouldBe "SkalAktiveres"
            varsel.skalAktiveres shouldBe true
            varsel.erAktivt shouldBe false
            varsel.harVærtAktivt shouldBe false
            varsel.erInaktivertEllerAvbrutt shouldBe false
            varsel.aktiveringstidspunkt shouldBe null
        }

        @Test
        fun `feiler med skalAktiveresTidspunkt før kl 9`() {
            assertThrows<IllegalArgumentException> {
                skalAktiveres(skalAktiveresTidspunkt = 6.januar(2025).atTime(8, 59))
            }
        }

        @Test
        fun `feiler med skalAktiveresTidspunkt etter kl 17`() {
            assertThrows<IllegalArgumentException> {
                skalAktiveres(skalAktiveresTidspunkt = 6.januar(2025).atHour(17))
            }
        }

        @Test
        fun `feiler med skalAktiveresTidspunkt på lørdag`() {
            assertThrows<IllegalArgumentException> {
                skalAktiveres(skalAktiveresTidspunkt = 4.januar(2025).atHour(10))
            }
        }

        @Test
        fun `feiler med skalAktiveresTidspunkt på søndag`() {
            assertThrows<IllegalArgumentException> {
                skalAktiveres(skalAktiveresTidspunkt = 5.januar(2025).atHour(10))
            }
        }

        @Test
        fun `godtar kl 9 og kl 16`() {
            skalAktiveres(skalAktiveresTidspunkt = 6.januar(2025).atHour(9))
            skalAktiveres(skalAktiveresTidspunkt = 6.januar(2025).atTime(16, 59))
        }

        @Test
        fun `aktiver returnerer Aktiv når tidspunkt er lik skalAktiveresTidspunkt`() {
            val varsel = skalAktiveres()
            val resultat = varsel.aktiver(gyldigAktiveringsTidspunkt)

            resultat.shouldBeInstanceOf<arrow.core.Either.Right<Varsel.Aktiv>>()
            val aktiv = resultat.getOrNull()!!
            aktiv.type shouldBe "Aktiv"
            aktiv.aktiveringstidspunkt shouldBe gyldigAktiveringsTidspunkt
            aktiv.erAktivt shouldBe true
            aktiv.harVærtAktivt shouldBe true
        }

        @Test
        fun `aktiver returnerer Aktiv når tidspunkt er etter skalAktiveresTidspunkt`() {
            val varsel = skalAktiveres()
            val senere = gyldigAktiveringsTidspunkt.plusHours(1)
            varsel.aktiver(senere).isRight() shouldBe true
        }

        @Test
        fun `aktiver returnerer ForTidlig når tidspunkt er før skalAktiveresTidspunkt`() {
            val varsel = skalAktiveres()
            val forTidlig = gyldigAktiveringsTidspunkt.minusSeconds(1)
            val resultat = varsel.aktiver(forTidlig)

            resultat.shouldBeInstanceOf<arrow.core.Either.Left<Varsel.KanIkkeAktivere.ForTidlig>>()
        }

        @Test
        fun `avbryt returnerer Avbrutt`() {
            val varsel = skalAktiveres()
            val avbruttTidspunkt = opprettet.plusHours(1)
            val avbrutt = varsel.avbryt(avbruttTidspunkt, "opphør")

            avbrutt.type shouldBe "Avbrutt"
            avbrutt.erAvbrutt shouldBe true
            avbrutt.erInaktivertEllerAvbrutt shouldBe true
            avbrutt.avbruttTidspunkt shouldBe avbruttTidspunkt
            avbrutt.avbruttBegrunnelse shouldBe "opphør"
            avbrutt.varselId shouldBe varselId
        }

        @Test
        fun `planleggPåNytt returnerer SkalAktiveres med oppdatert tidspunkt og begrunnelse`() {
            val varsel = skalAktiveres()
            val omplanlagtTidspunkt = 10.januar(2025).atHour(15)
            val omplanlagt = varsel.planleggPåNytt(
                skalAktiveresTidspunkt = omplanlagtTidspunkt,
                skalAktiveresBegrunnelse = "omplanlagt",
                sistEndret = 8.januar(2025).atHour(11),
            )

            omplanlagt.shouldBeInstanceOf<Varsel.SkalAktiveres>()
            omplanlagt.varselId shouldBe varsel.varselId
            omplanlagt.skalAktiveresTidspunkt shouldBe omplanlagtTidspunkt
            omplanlagt.skalAktiveresBegrunnelse shouldBe "omplanlagt"
        }

        @Test
        fun `planleggPåNytt feiler når nytt skalAktiveresTidspunkt er likt det eksisterende`() {
            val varsel = skalAktiveres()
            assertThrows<IllegalArgumentException> {
                varsel.planleggPåNytt(
                    skalAktiveresTidspunkt = varsel.skalAktiveresTidspunkt,
                    skalAktiveresBegrunnelse = "annen begrunnelse",
                    sistEndret = 8.januar(2025).atHour(11),
                )
            }
        }
    }

    @Nested
    inner class AktivTest {

        private fun aktiv(
            aktiveringstidspunkt: LocalDateTime = gyldigAktiveringsTidspunkt,
        ) = Varsel.Aktiv(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = gyldigAktiveringsTidspunkt,
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = aktiveringstidspunkt,
            opprettet = opprettet,
            sistEndret = aktiveringstidspunkt,
        )

        @Test
        fun `feiler med aktiveringstidspunkt før skalAktiveresTidspunkt`() {
            assertThrows<IllegalArgumentException> {
                aktiv(aktiveringstidspunkt = gyldigAktiveringsTidspunkt.minusSeconds(1))
            }
        }

        @Test
        fun `type og flagg er korrekte`() {
            val a = aktiv()
            a.type shouldBe "Aktiv"
            a.erAktivt shouldBe true
            a.harVærtAktivt shouldBe true
            a.skalAktiveres shouldBe false
            a.erInaktivert shouldBe false
            a.erAvbrutt shouldBe false
            a.erInaktivertEllerAvbrutt shouldBe false
        }

        @Test
        fun `forberedInaktivering returnerer SkalInaktiveres når tidspunkt er strengt etter aktivering`() {
            val a = aktiv()
            val resultat = a.forberedInaktivering(
                skalInaktiveresTidspunkt = gyldigAktiveringsTidspunkt.plusSeconds(1),
                skalInaktiveresBegrunnelse = "meldekort mottatt",
            )
            resultat.isRight() shouldBe true
            val skalInaktiveres = resultat.getOrNull()!!
            skalInaktiveres.type shouldBe "SkalInaktiveres"
            skalInaktiveres.erAktivt shouldBe true
            skalInaktiveres.skalInaktiveres shouldBe true
        }

        @Test
        fun `forberedInaktivering feiler når tidspunkt er lik aktiveringstidspunkt`() {
            val a = aktiv()
            val resultat = a.forberedInaktivering(
                skalInaktiveresTidspunkt = gyldigAktiveringsTidspunkt,
                skalInaktiveresBegrunnelse = "for tidlig",
            )
            resultat.shouldBeInstanceOf<arrow.core.Either.Left<Varsel.KanIkkeForberedeInaktivering.ForTidlig>>()
        }

        @Test
        fun `forberedInaktivering feiler når tidspunkt er før aktiveringstidspunkt`() {
            val a = aktiv()
            val resultat = a.forberedInaktivering(
                skalInaktiveresTidspunkt = gyldigAktiveringsTidspunkt.minusSeconds(1),
                skalInaktiveresBegrunnelse = "for tidlig",
            )
            resultat.isLeft() shouldBe true
        }
    }

    @Nested
    inner class SkalInaktiveresTest {

        private val aktivert = gyldigAktiveringsTidspunkt
        private val skalInaktiveresTid = aktivert.plusHours(1)

        private fun skalInaktiveres() = Varsel.SkalInaktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = gyldigAktiveringsTidspunkt,
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = aktivert,
            skalInaktiveresTidspunkt = skalInaktiveresTid,
            skalInaktiveresBegrunnelse = "meldekort mottatt",
            opprettet = opprettet,
            sistEndret = skalInaktiveresTid,
        )

        @Test
        fun `feiler med aktiveringstidspunkt før skalAktiveresTidspunkt`() {
            assertThrows<IllegalArgumentException> {
                Varsel.SkalInaktiveres(
                    sakId = sakId, saksnummer = saksnummer, fnr = fnr, varselId = varselId,
                    skalAktiveresTidspunkt = gyldigAktiveringsTidspunkt,
                    skalAktiveresBegrunnelse = "test",
                    aktiveringstidspunkt = gyldigAktiveringsTidspunkt.minusSeconds(1),
                    skalInaktiveresTidspunkt = gyldigAktiveringsTidspunkt.plusHours(1),
                    skalInaktiveresBegrunnelse = "test",
                    opprettet = opprettet, sistEndret = opprettet,
                )
            }
        }

        @Test
        fun `feiler med skalInaktiveresTidspunkt lik aktiveringstidspunkt`() {
            assertThrows<IllegalArgumentException> {
                Varsel.SkalInaktiveres(
                    sakId = sakId, saksnummer = saksnummer, fnr = fnr, varselId = varselId,
                    skalAktiveresTidspunkt = gyldigAktiveringsTidspunkt,
                    skalAktiveresBegrunnelse = "test",
                    aktiveringstidspunkt = gyldigAktiveringsTidspunkt,
                    skalInaktiveresTidspunkt = gyldigAktiveringsTidspunkt,
                    skalInaktiveresBegrunnelse = "test",
                    opprettet = opprettet, sistEndret = opprettet,
                )
            }
        }

        @Test
        fun `type og flagg er korrekte`() {
            val s = skalInaktiveres()
            s.type shouldBe "SkalInaktiveres"
            s.erAktivt shouldBe true
            s.skalInaktiveres shouldBe true
            s.harVærtAktivt shouldBe true
            s.erInaktivert shouldBe false
        }

        @Test
        fun `inaktiver returnerer Inaktivert når tidspunkt er lik skalInaktiveresTidspunkt`() {
            val s = skalInaktiveres()
            val resultat = s.inaktiver(skalInaktiveresTid)

            resultat.isRight() shouldBe true
            val inaktivert = resultat.getOrNull()!!
            inaktivert.type shouldBe "Inaktivert"
            inaktivert.erInaktivert shouldBe true
            inaktivert.erAktivt shouldBe false
            inaktivert.erInaktivertEllerAvbrutt shouldBe true
        }

        @Test
        fun `inaktiver returnerer Inaktivert når tidspunkt er etter skalInaktiveresTidspunkt`() {
            val s = skalInaktiveres()
            s.inaktiver(skalInaktiveresTid.plusSeconds(1)).isRight() shouldBe true
        }

        @Test
        fun `inaktiver returnerer ForTidlig når tidspunkt er før skalInaktiveresTidspunkt`() {
            val s = skalInaktiveres()
            val resultat = s.inaktiver(skalInaktiveresTid.minusSeconds(1))
            resultat.shouldBeInstanceOf<arrow.core.Either.Left<Varsel.KanIkkeInaktivere.ForTidlig>>()
        }
    }

    @Nested
    inner class InaktivertTest {

        @Test
        fun `type og flagg er korrekte`() {
            val i = Varsel.Inaktivert(
                sakId = sakId, saksnummer = saksnummer, fnr = fnr, varselId = varselId,
                skalAktiveresTidspunkt = gyldigAktiveringsTidspunkt,
                skalAktiveresBegrunnelse = "test",
                aktiveringstidspunkt = gyldigAktiveringsTidspunkt,
                skalInaktiveresTidspunkt = gyldigAktiveringsTidspunkt.plusHours(1),
                skalInaktiveresBegrunnelse = "mottatt",
                inaktiveringstidspunkt = gyldigAktiveringsTidspunkt.plusHours(1),
                opprettet = opprettet, sistEndret = opprettet,
            )
            i.type shouldBe "Inaktivert"
            i.erInaktivert shouldBe true
            i.erAktivt shouldBe false
            i.erInaktivertEllerAvbrutt shouldBe true
            i.harVærtAktivt shouldBe true
        }

        @Test
        fun `feiler med inaktiveringstidspunkt før skalInaktiveresTidspunkt`() {
            assertThrows<IllegalArgumentException> {
                Varsel.Inaktivert(
                    sakId = sakId, saksnummer = saksnummer, fnr = fnr, varselId = varselId,
                    skalAktiveresTidspunkt = gyldigAktiveringsTidspunkt,
                    skalAktiveresBegrunnelse = "test",
                    aktiveringstidspunkt = gyldigAktiveringsTidspunkt,
                    skalInaktiveresTidspunkt = gyldigAktiveringsTidspunkt.plusHours(1),
                    skalInaktiveresBegrunnelse = "mottatt",
                    inaktiveringstidspunkt = gyldigAktiveringsTidspunkt.plusMinutes(30),
                    opprettet = opprettet, sistEndret = opprettet,
                )
            }
        }
    }

    @Nested
    inner class AvbruttTest {

        @Test
        fun `type og flagg er korrekte`() {
            val a = Varsel.Avbrutt(
                sakId = sakId, saksnummer = saksnummer, fnr = fnr, varselId = varselId,
                skalAktiveresTidspunkt = gyldigAktiveringsTidspunkt,
                skalAktiveresBegrunnelse = "test",
                avbruttTidspunkt = opprettet.plusHours(1),
                avbruttBegrunnelse = "opphør",
                opprettet = opprettet, sistEndret = opprettet,
            )
            a.type shouldBe "Avbrutt"
            a.erAvbrutt shouldBe true
            a.erInaktivertEllerAvbrutt shouldBe true
            a.erAktivt shouldBe false
            a.harVærtAktivt shouldBe false
            a.aktiveringstidspunkt shouldBe null
        }
    }

    @Nested
    inner class FullTilstandsmaskinTest {

        @Test
        fun `full flyt SkalAktiveres til Inaktivert`() {
            val skalAktiveres = skalAktiveres()
            val aktiv = skalAktiveres.aktiver(gyldigAktiveringsTidspunkt).getOrNull()!!
            val skalInaktiveres = aktiv.forberedInaktivering(
                gyldigAktiveringsTidspunkt.plusHours(1),
                "meldekort mottatt",
            ).getOrNull()!!
            val inaktivert = skalInaktiveres.inaktiver(gyldigAktiveringsTidspunkt.plusHours(1)).getOrNull()!!

            inaktivert.erInaktivert shouldBe true
            inaktivert.varselId shouldBe varselId
        }

        @Test
        fun `full flyt SkalAktiveres til Avbrutt`() {
            val skalAktiveres = skalAktiveres()
            val avbrutt = skalAktiveres.avbryt(opprettet.plusHours(1), "opphør")

            avbrutt.erAvbrutt shouldBe true
            avbrutt.varselId shouldBe varselId
        }
    }

    @Nested
    inner class Tidspunktshjelpere {

        @Test
        fun `normaliserer til samme dag klokken ni før åpningstid`() {
            6.januar(2025).atTime(8, 59).nesteGyldigeVarseltidspunkt() shouldBe 6.januar(2025).atTime(9, 0)
        }

        @Test
        fun `normaliserer til neste virkedag klokken ni fra og med stengetid`() {
            10.januar(2025).atTime(17, 0).nesteGyldigeVarseltidspunkt() shouldBe 13.januar(2025).atTime(9, 0)
        }

        @Test
        fun `normaliserer til neste virkedag klokken ni i helg`() {
            4.januar(2025).atTime(12, 0).nesteGyldigeVarseltidspunkt() shouldBe 6.januar(2025).atTime(9, 0)
        }

        @Test
        fun `returnerer null for utsettSendingTil innenfor åpningstid`() {
            val tidspunkt = 6.januar(2025).atTime(10, 0)

            tidspunkt.utsettSendingTilHvisUtenforÅpningstid() shouldBe null
        }

        @Test
        fun `returnerer neste virkedag klokken ni for utsettSendingTil etter åpningstid`() {
            val tidspunkt = 10.januar(2025).atTime(18, 0)

            tidspunkt.utsettSendingTilHvisUtenforÅpningstid() shouldBe 13.januar(2025).atTime(9, 0)
        }
    }
}

private fun LocalDate.atHour(time: Int): LocalDateTime = this.atTime(time, 0)
