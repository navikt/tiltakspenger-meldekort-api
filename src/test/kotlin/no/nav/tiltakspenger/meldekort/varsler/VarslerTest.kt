package no.nav.tiltakspenger.meldekort.varsler

import arrow.core.left
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.meldekort.varsler.VarselId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
        skalAktiveresEksterntTidspunkt: LocalDateTime = skalAktiveresTidspunkt,
        varselId: VarselId = VarselId.random(),
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = nå(clock),
    ): Varsel.SkalAktiveres {
        return Varsel.SkalAktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
            skalAktiveresBegrunnelse = "test",
            opprettet = opprettet,
            sistEndret = opprettet,
        )
    }

    private fun aktiv(
        skalAktiveresTidspunkt: LocalDateTime = mandagKl10,
        aktiveringstidspunkt: LocalDateTime = skalAktiveresTidspunkt,
        eksternAktiveringstidspunkt: LocalDateTime = aktiveringstidspunkt,
        varselId: VarselId = VarselId.random(),
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = nå(clock),
    ): Varsel.Aktiv {
        return Varsel.Aktiv(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresEksterntTidspunkt = eksternAktiveringstidspunkt,
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = aktiveringstidspunkt,
            eksternAktiveringstidspunkt = eksternAktiveringstidspunkt,
            opprettet = opprettet,
            sistEndret = opprettet,
        )
    }

    private fun inaktivert(
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = nå(clock),
        eksternAktiveringstidspunkt: LocalDateTime = mandagKl10,
        inaktiveringstidspunkt: LocalDateTime = mandagKl10.plusHours(1),
    ): Varsel.Inaktivert {
        return Varsel.Inaktivert(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = mandagKl10,
            skalAktiveresEksterntTidspunkt = eksternAktiveringstidspunkt,
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = mandagKl10,
            eksternAktiveringstidspunkt = eksternAktiveringstidspunkt,
            skalInaktiveresTidspunkt = inaktiveringstidspunkt,
            skalInaktiveresBegrunnelse = "mottatt",
            inaktiveringstidspunkt = inaktiveringstidspunkt,
            opprettet = opprettet,
            sistEndret = opprettet,
        )
    }

    private fun skalInaktiveres(
        skalAktiveresTidspunkt: LocalDateTime = mandagKl10,
        aktiveringstidspunkt: LocalDateTime = skalAktiveresTidspunkt,
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = nå(clock),
    ): Varsel.SkalInaktiveres {
        return Varsel.SkalInaktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresEksterntTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = aktiveringstidspunkt,
            eksternAktiveringstidspunkt = aktiveringstidspunkt,
            skalInaktiveresTidspunkt = aktiveringstidspunkt.plusMinutes(1),
            skalInaktiveresBegrunnelse = "mottatt",
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
        fun `flere inaktiverte er gyldig`() {
            val clock = nyClock()

            varsler(inaktivert(clock = clock), inaktivert(clock = clock))
        }

        @Test
        fun `Aktiv og SkalInaktiveres kan sameksistere - pågående oppretting og pågående inaktivering`() {
            val clock = nyClock()
            val skalInaktiveres = skalInaktiveres(clock = clock)

            val pågående = varsler(
                skalInaktiveres,
                skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10, clock = clock),
            )

            pågående.pågåendeInaktiveringer shouldBe listOf(skalInaktiveres)
            pågående.pågåendeOppretting.shouldBeInstanceOf<Varsel.SkalAktiveres>()
        }

        @Test
        fun `to SkalAktiveres er ugyldig - kun ett pågående varsel tillatt`() {
            shouldThrow<IllegalArgumentException> {
                Varsler(
                    listOf(
                        skalAktiveres(skalAktiveresTidspunkt = mandagKl10),
                        skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10),
                    ),
                )
            }
        }

        @Test
        fun `SkalAktiveres og Aktiv samtidig er ugyldig`() {
            val clock = nyClock()
            shouldThrow<IllegalArgumentException> {
                varsler(
                    aktiv(skalAktiveresTidspunkt = mandagKl10, clock = clock),
                    skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10, clock = clock),
                )
            }
        }

        @Test
        fun `to SkalInaktiveres er gyldig`() {
            val clock = nyClock()
            val første = skalInaktiveres(clock = clock)
            val andre = skalInaktiveres(clock = clock)

            val varsler = varsler(første, andre)

            varsler.pågåendeInaktiveringer shouldBe listOf(første, andre)
        }
    }

    @Nested
    inner class ErAlleInaktivert {

        @Test
        fun `true for tom liste`() {
            Varsler(emptyList()).erAlleInaktivert shouldBe true
        }

        @Test
        fun `true når alle er inaktivert`() {
            val clock = nyClock()

            varsler(inaktivert(clock = clock), inaktivert(clock = clock)).erAlleInaktivert shouldBe true
        }

        @Test
        fun `false når det finnes et pågående varsel`() {
            val clock = nyClock()

            varsler(inaktivert(clock = clock), skalAktiveres(clock = clock)).erAlleInaktivert shouldBe false
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
        fun `kan legge til varsel når alle eksisterende er inaktivert`() {
            val clock = nyClock()
            val varsler = varsler(inaktivert(clock = clock), inaktivert(clock = clock))
            val resultat = varsler.leggTil(skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10, clock = clock))

            resultat.isRight() shouldBe true
            resultat.getOrNull()!! shouldHaveSize 3
        }

        @Test
        fun `kan ikke legge til varsel når det finnes et pågående`() {
            val varsler = Varsler(listOf(skalAktiveres(skalAktiveresTidspunkt = mandagKl10)))
            val resultat = varsler.leggTil(skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10))

            resultat shouldBe Varsler.KanIkkeLeggeTilVarsel.HarPågåendeOppretting.left()
        }

        @Test
        fun `kan legge til nytt SkalAktiveres når det finnes en pågående inaktivering`() {
            val clock = nyClock()
            val varsler = varsler(skalInaktiveres(clock = clock))
            val resultat = varsler.leggTil(skalAktiveres(skalAktiveresTidspunkt = tirsdagKl10, clock = clock))

            resultat.isRight() shouldBe true
            resultat.getOrNull()!! shouldHaveSize 2
        }

        @Test
        fun `kan legge til varsel selv om ekstern varsling allerede er antatt sendt samme dag`() {
            val eksisterendeOpprettet = 6.januar(2025).atHour(9)
            val eksisterende = inaktivert(opprettet = eksisterendeOpprettet)
            val varsler = Varsler(listOf(eksisterende))
            val nyttVarsel = skalAktiveres(
                skalAktiveresTidspunkt = mandagKl10.withHour(11),
                opprettet = eksisterendeOpprettet.plusMinutes(1),
            )

            val resultat = varsler.leggTil(nyttVarsel)

            resultat.isRight() shouldBe true
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
        fun `Inaktivert varsel aktivert på annen dag hindrer IKKE nytt varsel med samme planlagte dato`() {
            val eksisterendeOpprettet = 6.januar(2025).atHour(9)
            val inaktivertMenAktivertTirsdag = Varsel.Inaktivert(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                varselId = VarselId.random(),
                skalAktiveresTidspunkt = mandagKl10,
                skalAktiveresEksterntTidspunkt = mandagKl10,
                skalAktiveresBegrunnelse = "test",
                aktiveringstidspunkt = tirsdagKl10,
                eksternAktiveringstidspunkt = tirsdagKl10,
                skalInaktiveresTidspunkt = tirsdagKl10.plusHours(1),
                skalInaktiveresBegrunnelse = "mottatt",
                inaktiveringstidspunkt = tirsdagKl10.plusHours(1),
                opprettet = eksisterendeOpprettet,
                sistEndret = eksisterendeOpprettet,
            )
            val varsler = Varsler(listOf(inaktivertMenAktivertTirsdag))
            val nyttVarsel = skalAktiveres(
                skalAktiveresTidspunkt = 13.januar(2025).atHour(10),
                opprettet = eksisterendeOpprettet.plusMinutes(1),
            )

            val resultat = varsler.leggTil(nyttVarsel)

            resultat.isRight() shouldBe true
        }
    }

    @Nested
    inner class OppdaterVarsel {

        @Test
        fun `kan oppdatere eksisterende varsel`() {
            val opprettet = 6.januar(2025).atHour(9)
            val eksisterende = aktiv(
                skalAktiveresTidspunkt = mandagKl10,
                aktiveringstidspunkt = mandagKl10.plusMinutes(5),
                varselId = VarselId.random(),
                opprettet = opprettet,
            )
            val varsler = Varsler(listOf(eksisterende))

            val oppdatertVarsel = eksisterende.forberedInaktivering(
                skalInaktiveresTidspunkt = mandagKl10.plusHours(1),
                skalInaktiveresBegrunnelse = "mottatt",
            )

            val oppdatert = varsler.oppdater(oppdatertVarsel)

            oppdatert.single().shouldBeInstanceOf<Varsel.SkalInaktiveres>()
            oppdatert.single().varselId shouldBe eksisterende.varselId
        }

        @Test
        fun `kan ikke oppdatere ukjent varsel`() {
            val eksisterende = aktiv(varselId = VarselId.random(), opprettet = 6.januar(2025).atHour(9))
            val ukjent = aktiv(varselId = VarselId.random(), opprettet = 6.januar(2025).atHour(10))
            val varsler = Varsler(listOf(eksisterende))

            shouldThrow<IllegalArgumentException> {
                varsler.oppdater(ukjent)
            }
        }
    }

    @Nested
    inner class TilstandsovergangerViaAggregat {

        @Test
        fun `kan aktivere SkalAktiveres via Varsler`() {
            val opprettet = 6.januar(2025).atHour(9)
            val eksisterende = skalAktiveres(varselId = VarselId.random(), opprettet = opprettet)

            val oppdatert = Varsler(listOf(eksisterende)).aktiver(
                varselId = eksisterende.varselId,
                aktiveringstidspunkt = mandagKl10.plusMinutes(5),
            ).first

            oppdatert.single().shouldBeInstanceOf<Varsel.Aktiv>()
            oppdatert.single().varselId shouldBe eksisterende.varselId
        }

        @Test
        fun `kan forberede inaktivering via Varsler`() {
            val eksisterende = aktiv(varselId = VarselId.random(), opprettet = 6.januar(2025).atHour(9))

            val (_, skalInaktiveres) = Varsler(listOf(eksisterende)).forberedInaktivering(
                varselId = eksisterende.varselId,
                skalInaktiveresTidspunkt = mandagKl10.plusHours(1),
                skalInaktiveresBegrunnelse = "mottatt",
            )

            skalInaktiveres.shouldBeInstanceOf<Varsel.SkalInaktiveres>()
            skalInaktiveres.varselId shouldBe eksisterende.varselId
        }

        @Test
        fun `aktivt varsel kan forberedes for inaktivering mens et annet allerede skal inaktiveres`() {
            val alleredeTilInaktivering = skalInaktiveres(opprettet = 6.januar(2025).atHour(9))
            val aktivtVarsel = aktiv(
                varselId = VarselId.random(),
                skalAktiveresTidspunkt = tirsdagKl10,
                aktiveringstidspunkt = tirsdagKl10,
                opprettet = 6.januar(2025).atTime(9, 1),
            )
            val varsler = Varsler(listOf(alleredeTilInaktivering, aktivtVarsel))

            val (oppdaterteVarsler, nyTilInaktivering) = varsler.forberedInaktivering(
                varselId = aktivtVarsel.varselId,
                skalInaktiveresTidspunkt = tirsdagKl10.plusHours(1),
                skalInaktiveresBegrunnelse = "mottatt meldekort",
            )

            nyTilInaktivering.varselId shouldBe aktivtVarsel.varselId
            oppdaterteVarsler.pågåendeInaktiveringer.map { it.varselId } shouldBe listOf(
                alleredeTilInaktivering.varselId,
                aktivtVarsel.varselId,
            )
        }

        @Test
        fun `kan inaktivere via Varsler`() {
            val eksisterende = skalInaktiveres(opprettet = 6.januar(2025).atHour(9))

            val oppdatert = Varsler(listOf(eksisterende)).inaktiver(
                varselId = eksisterende.varselId,
                inaktiveringstidspunkt = mandagKl10.plusHours(1),
            ).first

            oppdatert.single().shouldBeInstanceOf<Varsel.Inaktivert>()
            oppdatert.single().varselId shouldBe eksisterende.varselId
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
                skalAktiveresEksterntTidspunkt = mandagKl10,
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
    inner class EksternVarslingstidspunkt {

        @Test
        fun `bruker ønsket tidspunkt når det allerede er gyldig og ingen varsel er sendt samme dag`() {
            Varsler(emptyList()).finnTidspunktForEksternVarsling(
                ønsketTidspunkt = tirsdagKl10,
                nå = mandagKl10,
            ) shouldBe tirsdagKl10
        }

        @Test
        fun `bruker nå normalisert til åpningstid når ønsket tidspunkt er passert`() {
            Varsler(emptyList()).finnTidspunktForEksternVarsling(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = 6.januar(2025).atTime(8, 30),
            ) shouldBe 6.januar(2025).atHour(9)
        }

        @Test
        fun `utsetter til neste virkedag når ekstern varsling allerede er antatt sendt samme dag`() {
            val aktivtVarsel = aktiv(
                skalAktiveresTidspunkt = 6.januar(2025).atHour(9),
                aktiveringstidspunkt = 6.januar(2025).atTime(9, 5),
                eksternAktiveringstidspunkt = 6.januar(2025).atTime(9, 5),
                opprettet = 6.januar(2025).atHour(9),
            )

            Varsler(listOf(aktivtVarsel)).finnTidspunktForEksternVarsling(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = 6.januar(2025).atHour(10),
            ) shouldBe 7.januar(2025).atHour(9)
        }

        @Test
        fun `utsetter ikke når tidligere varsel ble inaktivert før ekstern varsling kunne gå ut`() {
            val inaktivertFørEksternVarsling = inaktivert(
                opprettet = 6.januar(2025).atHour(9),
                eksternAktiveringstidspunkt = 6.januar(2025).atHour(15),
                inaktiveringstidspunkt = 6.januar(2025).atTime(14, 30),
            )
            val nå = 6.januar(2025).atTime(14, 30)

            Varsler(listOf(inaktivertFørEksternVarsling)).finnTidspunktForEksternVarsling(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = nå,
            ) shouldBe nå
        }

        @Test
        fun `utsetter når tidligere varsel ble inaktivert rett før ekstern varslingstidspunkt`() {
            val inaktivertRettFørEksternVarsling = inaktivert(
                opprettet = 6.januar(2025).atHour(9),
                eksternAktiveringstidspunkt = 6.januar(2025).atHour(15),
                inaktiveringstidspunkt = 6.januar(2025).atTime(14, 50),
            )

            Varsler(listOf(inaktivertRettFørEksternVarsling)).finnTidspunktForEksternVarsling(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = 6.januar(2025).atTime(14, 50),
            ) shouldBe 7.januar(2025).atHour(9)
        }

        @Test
        fun `historisk skalAktiveresTidspunkt blokkerer ikke ekstern varsling i dag`() {
            val tidligereVarsel = inaktivert(
                opprettet = 6.januar(2025).atHour(9),
                eksternAktiveringstidspunkt = 6.januar(2025).atHour(10),
                inaktiveringstidspunkt = 6.januar(2025).atHour(11),
            )
            val nå = 13.januar(2025).atHour(10)

            Varsler(listOf(tidligereVarsel)).finnTidspunktForEksternVarsling(
                ønsketTidspunkt = 6.januar(2025).atHour(10),
                nå = nå,
            ) shouldBe nå
        }

        @Test
        fun `bruker nå når ønsket tidspunkt er passert og nå er innenfor åpningstid`() {
            val nå = 6.januar(2025).atTime(10, 30)

            Varsler(emptyList()).finnTidspunktForEksternVarsling(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = nå,
            ) shouldBe nå
        }

        @Test
        fun `bruker neste virkedag klokken ni når nå er etter åpningstid`() {
            Varsler(emptyList()).finnTidspunktForEksternVarsling(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = 3.januar(2025).atHour(18),
            ) shouldBe 6.januar(2025).atHour(9)
        }

        @Test
        fun `bruker neste virkedag klokken ni når nå er i helgen`() {
            Varsler(emptyList()).finnTidspunktForEksternVarsling(
                ønsketTidspunkt = 3.januar(2025).atHour(15),
                nå = 4.januar(2025).atHour(12),
            ) shouldBe 6.januar(2025).atHour(9)
        }
    }
}

private fun java.time.LocalDate.atHour(time: Int): LocalDateTime = this.atTime(time, 0)
