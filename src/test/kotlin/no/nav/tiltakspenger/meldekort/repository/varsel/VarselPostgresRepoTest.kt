package no.nav.tiltakspenger.meldekort.repository.varsel

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class VarselPostgresRepoTest {
    private val startTid = 6.januar(2025).atHour(9)

    private fun nyClock(): Clock = TikkendeKlokke(fixedClockAt(startTid))

    @Test
    fun `lagre og hentForSakId roundtripper alle varseltyper`() {
        withMigratedDb(clock = fixedClockAt(startTid)) { helper ->
            val varselPerSak = listOf(
                skalAktiveres(saksnummer = "sak-1"),
                aktiv(saksnummer = "sak-2"),
                skalInaktiveres(saksnummer = "sak-3"),
                inaktivert(saksnummer = "sak-4"),
                avbrutt(saksnummer = "sak-5"),
            ).map { varsel -> varsel.sakId to varsel }

            varselPerSak.forEach { (sakId, varsel) ->
                lagreSak(helper, sakId = sakId, saksnummer = varsel.saksnummer, fnr = varsel.fnr)
                helper.varselPostgresRepo.lagre(varsel)
            }

            varselPerSak.forEach { (sakId, expected) ->
                helper.varselPostgresRepo.hentForSakId(sakId).single() shouldBe expected
            }
        }
    }

    @Test
    fun `hentForSakId returnerer varsler sortert på opprettet`() {
        withMigratedDb(clock = fixedClockAt(startTid)) { helper ->
            val sakId = SakId.random()
            val fnr = Fnr.random()
            lagreSak(helper, sakId = sakId, saksnummer = "sak-sortert", fnr = fnr)
            val tidlig = inaktivert(
                sakId = sakId,
                saksnummer = "sak-sortert",
                fnr = fnr,
                skalAktiveresTidspunkt = startTid.plusDays(1),
                opprettet = startTid.plusMinutes(1),
            )
            val mellom = avbrutt(
                sakId = sakId,
                saksnummer = "sak-sortert",
                fnr = fnr,
                skalAktiveresTidspunkt = startTid.plusDays(2),
                opprettet = startTid.plusMinutes(2),
            )
            val sen = skalAktiveres(
                sakId = sakId,
                saksnummer = "sak-sortert",
                fnr = fnr,
                skalAktiveresTidspunkt = startTid.plusDays(3),
                opprettet = startTid,
            )

            helper.varselPostgresRepo.lagre(sen)
            helper.varselPostgresRepo.lagre(tidlig)
            helper.varselPostgresRepo.lagre(mellom)

            val resultat = helper.varselPostgresRepo.hentForSakId(sakId)

            resultat.map { it.varselId } shouldContainExactly listOf(sen.varselId, tidlig.varselId, mellom.varselId)
        }
    }

    @Test
    fun `lagre oppdaterer eksisterende varsel og bevarer metadata ved null med coalesce`() {
        withMigratedDb(clock = fixedClockAt(startTid)) { helper ->
            val sakId = SakId.random()
            val varselId = VarselId.random()
            val fnr = Fnr.random()
            lagreSak(helper, sakId = sakId, saksnummer = "sak-upsert", fnr = fnr)
            val opprinneligVarsel = skalAktiveres(
                sakId = sakId,
                saksnummer = "sak-upsert",
                varselId = varselId,
                fnr = fnr,
                skalAktiveresTidspunkt = startTid.plusHours(1),
            )
            val omplanlagtVarsel = opprinneligVarsel.planleggPåNytt(
                skalAktiveresTidspunkt = startTid.plusHours(3),
                skalAktiveresBegrunnelse = "omplanlagt etter ny vurdering",
                sistEndret = startTid.plusMinutes(30),
            )
            val aktivtVarsel = omplanlagtVarsel.aktiver(omplanlagtVarsel.skalAktiveresTidspunkt.plusMinutes(5)).getOrNull()!!
            val skalInaktiveresVarsel = aktivtVarsel.forberedInaktivering(
                skalInaktiveresTidspunkt = aktivtVarsel.aktiveringstidspunkt.plusHours(1),
                skalInaktiveresBegrunnelse = "mottatt",
            ).getOrNull()!!

            helper.varselPostgresRepo.lagre(opprinneligVarsel)
            helper.varselPostgresRepo.lagre(omplanlagtVarsel)
            helper.varselPostgresRepo.lagre(aktivtVarsel, aktiveringsmetadata = "aktivering-1")
            helper.varselPostgresRepo.lagre(skalInaktiveresVarsel, inaktiveringsmetadata = "inaktivering-1")

            hentMetadata(helper, varselId) shouldBe VarselMetadata(
                type = "SkalInaktiveres",
                aktiveringsmetadata = "aktivering-1",
                inaktiveringsmetadata = "inaktivering-1",
                skalAktiveresTidspunkt = startTid.plusHours(3),
                skalAktiveresBegrunnelse = "omplanlagt etter ny vurdering",
            )
        }
    }

    @Test
    fun `hentVarslerSomSkalAktiveres filtrerer på tidspunkt sorterer og respekterer limit`() {
        val klokke = 6.januar(2025).atHour(12)
        withMigratedDb(clock = fixedClockAt(klokke)) { helper ->
            val forfallerTidlig = skalAktiveres(sakId = SakId.random(), saksnummer = "sak-a", fnr = Fnr.random(), skalAktiveresTidspunkt = klokke.minusHours(2))
            val forfallerSent = skalAktiveres(sakId = SakId.random(), saksnummer = "sak-b", fnr = Fnr.random(), skalAktiveresTidspunkt = klokke.minusHours(1))
            val fremtidig = skalAktiveres(sakId = SakId.random(), saksnummer = "sak-c", fnr = Fnr.random(), skalAktiveresTidspunkt = klokke.plusHours(1))
            val feilType = aktiv(sakId = SakId.random(), saksnummer = "sak-d", fnr = Fnr.random(), skalAktiveresTidspunkt = klokke.minusHours(3))

            listOf(forfallerTidlig, forfallerSent, fremtidig, feilType).forEach { varsel ->
                lagreSak(helper, sakId = varsel.sakId, saksnummer = varsel.saksnummer, fnr = varsel.fnr)
                helper.varselPostgresRepo.lagre(varsel)
            }

            val resultat = helper.varselPostgresRepo.hentVarslerSomSkalAktiveres(limit = 2)

            resultat.map { it.varselId } shouldContainExactly listOf(forfallerTidlig.varselId, forfallerSent.varselId)
        }
    }

    @Test
    fun `hentVarslerSomSkalInaktiveres filtrerer på tidspunkt sorterer og respekterer limit`() {
        val klokke = 6.januar(2025).atHour(12)
        withMigratedDb(clock = fixedClockAt(klokke)) { helper ->
            val forfallerTidlig = skalInaktiveres(sakId = SakId.random(), saksnummer = "sak-e", fnr = Fnr.random(), skalAktiveresTidspunkt = 6.januar(2025).atTime(9, 30))
            val forfallerSent = skalInaktiveres(sakId = SakId.random(), saksnummer = "sak-f", fnr = Fnr.random(), skalAktiveresTidspunkt = 6.januar(2025).atTime(10, 50))
            val fremtidig = skalInaktiveres(sakId = SakId.random(), saksnummer = "sak-g", fnr = Fnr.random(), skalAktiveresTidspunkt = 6.januar(2025).atTime(11, 30))
            val feilType = inaktivert(sakId = SakId.random(), saksnummer = "sak-h", fnr = Fnr.random(), skalAktiveresTidspunkt = 6.januar(2025).atHour(9))

            listOf(forfallerTidlig, forfallerSent, fremtidig, feilType).forEach { varsel ->
                lagreSak(helper, sakId = varsel.sakId, saksnummer = varsel.saksnummer, fnr = varsel.fnr)
                helper.varselPostgresRepo.lagre(varsel)
            }

            val resultat = helper.varselPostgresRepo.hentVarslerSomSkalInaktiveres(limit = 2)

            resultat.map { it.varselId } shouldContainExactly listOf(forfallerTidlig.varselId, forfallerSent.varselId)
        }
    }

    private fun lagreSak(
        helper: TestDataHelper,
        sakId: SakId,
        saksnummer: String,
        fnr: Fnr,
    ) {
        helper.sakPostgresRepo.lagre(
            ObjectMother.sak(
                id = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                meldeperioder = emptyList(),
            ),
        )
    }

    private fun hentMetadata(helper: TestDataHelper, varselId: VarselId): VarselMetadata {
        return helper.sessionFactory.withSession(null) { session ->
            session.run(
                sqlQuery(
                    """
                    select type, aktiveringsmetadata, inaktiveringsmetadata, skal_aktiveres_tidspunkt, skal_aktiveres_begrunnelse
                    from varsel
                    where varsel_id = :varsel_id
                    """.trimIndent(),
                    "varsel_id" to varselId.toString(),
                ).map { row ->
                    VarselMetadata(
                        type = row.string("type"),
                        aktiveringsmetadata = row.stringOrNull("aktiveringsmetadata"),
                        inaktiveringsmetadata = row.stringOrNull("inaktiveringsmetadata"),
                        skalAktiveresTidspunkt = row.localDateTime("skal_aktiveres_tidspunkt"),
                        skalAktiveresBegrunnelse = row.string("skal_aktiveres_begrunnelse"),
                    )
                }.asSingle,
            )!!
        }
    }

    private fun skalAktiveres(
        sakId: SakId = SakId.random(),
        saksnummer: String,
        varselId: VarselId = VarselId.random(),
        fnr: Fnr = Fnr.random(),
        skalAktiveresTidspunkt: LocalDateTime = 6.januar(2025).atHour(10),
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

    private fun aktiv(
        sakId: SakId = SakId.random(),
        saksnummer: String,
        varselId: VarselId = VarselId.random(),
        fnr: Fnr = Fnr.random(),
        skalAktiveresTidspunkt: LocalDateTime = 6.januar(2025).atHour(10),
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
    ): Varsel.Aktiv {
        return skalAktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            varselId = varselId,
            fnr = fnr,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            clock = clock,
            opprettet = opprettet,
        ).aktiver(skalAktiveresTidspunkt.plusMinutes(5)).getOrNull()!!
    }

    private fun skalInaktiveres(
        sakId: SakId = SakId.random(),
        saksnummer: String,
        varselId: VarselId = VarselId.random(),
        fnr: Fnr = Fnr.random(),
        skalAktiveresTidspunkt: LocalDateTime = 6.januar(2025).atHour(10),
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
    ): Varsel.SkalInaktiveres {
        val aktivtVarsel = aktiv(
            sakId = sakId,
            saksnummer = saksnummer,
            varselId = varselId,
            fnr = fnr,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            clock = clock,
            opprettet = opprettet,
        )
        return aktivtVarsel.forberedInaktivering(
            skalInaktiveresTidspunkt = aktivtVarsel.aktiveringstidspunkt.plusHours(1),
            skalInaktiveresBegrunnelse = "test",
        ).getOrNull()!!
    }

    private fun inaktivert(
        sakId: SakId = SakId.random(),
        saksnummer: String,
        varselId: VarselId = VarselId.random(),
        fnr: Fnr = Fnr.random(),
        skalAktiveresTidspunkt: LocalDateTime = 6.januar(2025).atHour(10),
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
    ): Varsel.Inaktivert {
        val skalInaktiveresVarsel = skalInaktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            varselId = varselId,
            fnr = fnr,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            clock = clock,
            opprettet = opprettet,
        )
        return skalInaktiveresVarsel.inaktiver(skalInaktiveresVarsel.skalInaktiveresTidspunkt).getOrNull()!!
    }

    private fun avbrutt(
        sakId: SakId = SakId.random(),
        saksnummer: String,
        varselId: VarselId = VarselId.random(),
        fnr: Fnr = Fnr.random(),
        skalAktiveresTidspunkt: LocalDateTime = 6.januar(2025).atHour(10),
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
    ): Varsel.Avbrutt {
        return skalAktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            varselId = varselId,
            fnr = fnr,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            clock = clock,
            opprettet = opprettet,
        ).avbryt(
            avbruttTidspunkt = skalAktiveresTidspunkt.minusMinutes(5),
            avbruttBegrunnelse = "test",
        )
    }

    private data class VarselMetadata(
        val type: String,
        val aktiveringsmetadata: String?,
        val inaktiveringsmetadata: String?,
        val skalAktiveresTidspunkt: LocalDateTime,
        val skalAktiveresBegrunnelse: String,
    )
}

private fun LocalDate.atHour(time: Int): LocalDateTime = this.atTime(time, 0)
