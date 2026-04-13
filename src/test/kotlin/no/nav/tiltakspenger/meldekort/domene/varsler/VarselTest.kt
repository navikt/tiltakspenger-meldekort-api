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
        skalAktiveresEksterntTidspunkt = skalAktiveresTidspunkt,
        skalAktiveresBegrunnelse = "test",
        opprettet = opprettet,
        sistEndret = opprettet,
    )

    @Nested
    inner class SkalAktiveresTest {

        @Test
        fun `kan opprettes med gyldig tidspunkt på ukedag innenfor kontortid`() {
            val varsel = skalAktiveres()
            varsel.erInaktivert shouldBe false
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
        fun `aktiver returnerer Aktiv`() {
            val varsel = skalAktiveres()
            val aktiv = varsel.aktiver(gyldigAktiveringsTidspunkt)

            aktiv.aktiveringstidspunkt shouldBe gyldigAktiveringsTidspunkt
        }

        @Test
        fun `aktiver feiler dersom aktiveringstidspunkt er før skalAktiveresTidspunkt`() {
            val varsel = skalAktiveres(skalAktiveresTidspunkt = 10.januar(2025).atHour(9))
            val nå = 6.januar(2025).atHour(10)

            assertThrows<IllegalArgumentException> {
                varsel.aktiver(nå)
            }
        }

        @Test
        fun `feiler når skalAktiveresEksterntTidspunkt er før skalAktiveresTidspunkt`() {
            assertThrows<IllegalArgumentException> {
                Varsel.SkalAktiveres(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    varselId = varselId,
                    skalAktiveresTidspunkt = 7.januar(2025).atHour(10),
                    skalAktiveresEksterntTidspunkt = 6.januar(2025).atHour(10),
                    skalAktiveresBegrunnelse = "test",
                    opprettet = opprettet,
                    sistEndret = opprettet,
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
            skalAktiveresEksterntTidspunkt = gyldigAktiveringsTidspunkt,
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = aktiveringstidspunkt,
            eksternAktiveringstidspunkt = aktiveringstidspunkt,
            opprettet = opprettet,
            sistEndret = aktiveringstidspunkt,
        )

        @Test
        fun `type og flagg er korrekte`() {
            val a = aktiv()
            a.erInaktivert shouldBe false
        }

        @Test
        fun `forberedInaktivering returnerer SkalInaktiveres`() {
            val a = aktiv()
            val skalInaktiveres = a.forberedInaktivering(
                skalInaktiveresTidspunkt = gyldigAktiveringsTidspunkt.plusSeconds(1),
                skalInaktiveresBegrunnelse = "meldekort mottatt",
            )
            skalInaktiveres.shouldBeInstanceOf<Varsel.SkalInaktiveres>()
            skalInaktiveres.skalInaktiveresTidspunkt shouldBe gyldigAktiveringsTidspunkt.plusSeconds(1)
            skalInaktiveres.skalInaktiveresBegrunnelse shouldBe "meldekort mottatt"
        }

        @Test
        fun `forberedInaktivering tillater tidspunkt lik aktiveringstidspunkt - kan inaktivere umiddelbart`() {
            val a = aktiv()
            val skalInaktiveres = a.forberedInaktivering(
                skalInaktiveresTidspunkt = gyldigAktiveringsTidspunkt,
                skalInaktiveresBegrunnelse = "umiddelbar inaktivering",
            )
            skalInaktiveres.skalInaktiveresTidspunkt shouldBe gyldigAktiveringsTidspunkt
        }

        @Test
        fun `feiler når aktiveringstidspunkt er før skalAktiveresTidspunkt`() {
            assertThrows<IllegalArgumentException> {
                Varsel.Aktiv(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    varselId = varselId,
                    skalAktiveresTidspunkt = 7.januar(2025).atHour(10),
                    skalAktiveresEksterntTidspunkt = 7.januar(2025).atHour(10),
                    skalAktiveresBegrunnelse = "test",
                    aktiveringstidspunkt = 6.januar(2025).atHour(10),
                    eksternAktiveringstidspunkt = 7.januar(2025).atHour(10),
                    opprettet = opprettet,
                    sistEndret = 6.januar(2025).atHour(10),
                )
            }
        }

        @Test
        fun `feiler når eksternAktiveringstidspunkt er før aktiveringstidspunkt`() {
            assertThrows<IllegalArgumentException> {
                Varsel.Aktiv(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    varselId = varselId,
                    skalAktiveresTidspunkt = 6.januar(2025).atHour(10),
                    skalAktiveresEksterntTidspunkt = 6.januar(2025).atHour(10),
                    skalAktiveresBegrunnelse = "test",
                    aktiveringstidspunkt = 7.januar(2025).atHour(10),
                    eksternAktiveringstidspunkt = 6.januar(2025).atHour(10),
                    opprettet = opprettet,
                    sistEndret = 7.januar(2025).atHour(10),
                )
            }
        }

        @Test
        fun `feiler når skalAktiveresEksterntTidspunkt er før skalAktiveresTidspunkt`() {
            assertThrows<IllegalArgumentException> {
                Varsel.Aktiv(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    varselId = varselId,
                    skalAktiveresTidspunkt = 7.januar(2025).atHour(10),
                    skalAktiveresEksterntTidspunkt = 6.januar(2025).atHour(10),
                    skalAktiveresBegrunnelse = "test",
                    aktiveringstidspunkt = 7.januar(2025).atHour(10),
                    eksternAktiveringstidspunkt = 7.januar(2025).atHour(10),
                    opprettet = opprettet,
                    sistEndret = 7.januar(2025).atHour(10),
                )
            }
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
            skalAktiveresEksterntTidspunkt = gyldigAktiveringsTidspunkt,
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = aktivert,
            eksternAktiveringstidspunkt = aktivert,
            skalInaktiveresTidspunkt = skalInaktiveresTid,
            skalInaktiveresBegrunnelse = "meldekort mottatt",
            opprettet = opprettet,
            sistEndret = skalInaktiveresTid,
        )

        @Test
        fun `type og flagg er korrekte`() {
            val s = skalInaktiveres()
            s.erInaktivert shouldBe false
        }

        @Test
        fun `inaktiver returnerer Inaktivert`() {
            val s = skalInaktiveres()
            val inaktivert = s.inaktiver(skalInaktiveresTid)

            inaktivert.erInaktivert shouldBe true
        }
    }

    @Nested
    inner class InaktivertTest {

        @Test
        fun `type og flagg er korrekte`() {
            val i = Varsel.Inaktivert(
                sakId = sakId, saksnummer = saksnummer, fnr = fnr, varselId = varselId,
                skalAktiveresTidspunkt = gyldigAktiveringsTidspunkt,
                skalAktiveresEksterntTidspunkt = gyldigAktiveringsTidspunkt,
                skalAktiveresBegrunnelse = "test",
                aktiveringstidspunkt = gyldigAktiveringsTidspunkt,
                eksternAktiveringstidspunkt = gyldigAktiveringsTidspunkt,
                skalInaktiveresTidspunkt = gyldigAktiveringsTidspunkt.plusHours(1),
                skalInaktiveresBegrunnelse = "mottatt",
                inaktiveringstidspunkt = gyldigAktiveringsTidspunkt.plusHours(1),
                opprettet = opprettet, sistEndret = opprettet,
            )
            i.erInaktivert shouldBe true
        }
    }

    @Nested
    inner class FullTilstandsmaskinTest {

        @Test
        fun `full flyt SkalAktiveres til Inaktivert`() {
            val skalAktiveres = skalAktiveres()
            val aktiv = skalAktiveres.aktiver(gyldigAktiveringsTidspunkt)
            val skalInaktiveres = aktiv.forberedInaktivering(
                gyldigAktiveringsTidspunkt.plusHours(1),
                "meldekort mottatt",
            )
            val inaktivert = skalInaktiveres.inaktiver(gyldigAktiveringsTidspunkt.plusHours(1))

            inaktivert.erInaktivert shouldBe true
            inaktivert.varselId shouldBe varselId
        }
    }

    @Nested
    inner class Tidspunktshjelpere {

        @Test
        fun `normaliserer til samme dag klokken ni før åpningstid`() {
            6.januar(2025).atTime(8, 59).nesteGyldigeEksternVarseltidspunkt() shouldBe 6.januar(2025).atTime(9, 0)
        }

        @Test
        fun `normaliserer til neste virkedag klokken ni fra og med stengetid`() {
            10.januar(2025).atTime(17, 0).nesteGyldigeEksternVarseltidspunkt() shouldBe 13.januar(2025).atTime(9, 0)
        }

        @Test
        fun `normaliserer til neste virkedag klokken ni i helg`() {
            4.januar(2025).atTime(12, 0).nesteGyldigeEksternVarseltidspunkt() shouldBe 6.januar(2025).atTime(9, 0)
        }
    }
}

private fun LocalDate.atHour(time: Int): LocalDateTime = this.atTime(time, 0)
