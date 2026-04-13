package no.nav.tiltakspenger.meldekort.repository.varsel

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class VarselPostgresRepoTest {
    private val startTid = 6.januar(2025).atHour(9)
    private var opprettetOffsetMicros = 0L

    private fun nyClock(): Clock = TikkendeKlokke(fixedClockAt(startTid))

    private fun nesteOpprettet(): LocalDateTime {
        opprettetOffsetMicros += 1
        return startTid.plusNanos(opprettetOffsetMicros * 1000)
    }

    @Test
    fun `lagre og hentForSakId roundtripper alle varseltyper`() {
        withMigratedDb(clock = fixedClockAt(startTid)) { helper ->
            val varselPerSak = listOf(
                skalAktiveres(saksnummer = "sak-1"),
                aktiv(saksnummer = "sak-2"),
                skalInaktiveres(saksnummer = "sak-3"),
                inaktivert(saksnummer = "sak-4"),
            ).map { varsel -> varsel.sakId to varsel }

            varselPerSak.forEach { (sakId, varsel) ->
                lagreSak(helper, sakId = sakId, saksnummer = varsel.saksnummer, fnr = varsel.fnr)
                helper.varselPostgresRepo.lagre(varsel)
            }

            varselPerSak.forEach { (sakId, expected) ->
                helper.varselPostgresRepo.hentVarslerForSakId(sakId).single() shouldBe expected
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
            val sen = skalAktiveres(
                sakId = sakId,
                saksnummer = "sak-sortert",
                fnr = fnr,
                skalAktiveresTidspunkt = startTid.plusDays(3),
                opprettet = startTid,
            )

            helper.varselPostgresRepo.lagre(sen)
            helper.varselPostgresRepo.lagre(tidlig)

            val resultat = helper.varselPostgresRepo.hentVarslerForSakId(sakId)

            resultat.map { it.varselId } shouldContainExactly listOf(sen.varselId, tidlig.varselId)
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
            val aktivtVarsel = opprinneligVarsel.aktiver(opprinneligVarsel.skalAktiveresTidspunkt.plusMinutes(5))
            val skalInaktiveresVarsel = aktivtVarsel.forberedInaktivering(
                skalInaktiveresTidspunkt = aktivtVarsel.aktiveringstidspunkt.plusHours(1),
                skalInaktiveresBegrunnelse = "mottatt",
            )

            helper.varselPostgresRepo.lagre(opprinneligVarsel)
            helper.varselPostgresRepo.lagre(aktivtVarsel, aktiveringsmetadata = "aktivering-1")
            helper.varselPostgresRepo.lagre(skalInaktiveresVarsel, inaktiveringsmetadata = "inaktivering-1")

            hentMetadata(helper, varselId) shouldBe VarselMetadata(
                type = "SkalInaktiveres",
                aktiveringsmetadata = "aktivering-1",
                inaktiveringsmetadata = "inaktivering-1",
                skalAktiveresTidspunkt = startTid.plusHours(1),
                skalAktiveresBegrunnelse = "test",
            )
        }
    }

    @Test
    fun `hentVarslerSomSkalAktiveres returnerer alle SkalAktiveres sortert på opprettet og respekterer limit`() {
        val klokke = 6.januar(2025).atHour(12)
        withMigratedDb(clock = fixedClockAt(klokke)) { helper ->
            // hentSakerMedVarslerSomSkalAktiveres filtrerer på skal_aktiveres_tidspunkt <= nå,
            // så alle kandidatene må ha skalAktiveresTidspunkt i fortiden for at testen skal
            // dekke sortering på opprettet og limit. Tidspunktene må også ligge innenfor
            // 09:00-17:00 på en virkedag pga. invarianter på Varsel.SkalAktiveres.
            val eldst = skalAktiveres(sakId = SakId.random(), saksnummer = "sak-a", fnr = Fnr.random(), skalAktiveresTidspunkt = klokke.minusHours(3), opprettet = klokke.minusHours(3))
            val mellom = skalAktiveres(sakId = SakId.random(), saksnummer = "sak-b", fnr = Fnr.random(), skalAktiveresTidspunkt = klokke.minusHours(2), opprettet = klokke.minusHours(2))
            val yngst = skalAktiveres(sakId = SakId.random(), saksnummer = "sak-c", fnr = Fnr.random(), skalAktiveresTidspunkt = klokke.minusHours(1), opprettet = klokke.minusHours(1))
            val feilType = aktiv(sakId = SakId.random(), saksnummer = "sak-d", fnr = Fnr.random(), skalAktiveresTidspunkt = klokke.minusHours(3))

            listOf(eldst, mellom, yngst, feilType).forEach { varsel ->
                lagreSak(helper, sakId = varsel.sakId, saksnummer = varsel.saksnummer, fnr = varsel.fnr)
                helper.varselPostgresRepo.lagre(varsel)
            }

            val resultat = helper.varselPostgresRepo.hentSakerMedVarslerSomSkalAktiveres(limit = 2)

            resultat shouldContainExactly listOf(eldst.sakId, mellom.sakId)
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

            val resultat = helper.varselPostgresRepo.hentSakerMedVarslerSomSkalInaktiveres(limit = 2)

            resultat shouldContainExactly listOf(forfallerTidlig.sakId, forfallerSent.sakId)
        }
    }

    @Test
    fun `lagre avviser flere pågående varsler for samme sak`() {
        withMigratedDb(clock = fixedClockAt(startTid)) { helper ->
            val sakId = SakId.random()
            val fnr = Fnr.random()
            lagreSak(helper, sakId = sakId, saksnummer = "sak-unik-oppretting", fnr = fnr)
            helper.varselPostgresRepo.lagre(
                skalAktiveres(
                    sakId = sakId,
                    saksnummer = "sak-unik-oppretting",
                    fnr = fnr,
                    skalAktiveresTidspunkt = startTid.plusHours(1),
                    opprettet = startTid,
                ),
            )

            assertThrows<Throwable> {
                helper.varselPostgresRepo.lagre(
                    aktiv(
                        sakId = sakId,
                        saksnummer = "sak-unik-oppretting",
                        fnr = fnr,
                        skalAktiveresTidspunkt = startTid.plusHours(2),
                        opprettet = startTid.plusMinutes(1),
                    ),
                )
            }
        }
    }

    @Test
    fun `lagre tillater samme opprettet for ulike saker`() {
        withMigratedDb(clock = fixedClockAt(startTid)) { helper ->
            val fellesOpprettet = startTid
            val første = inaktivert(sakId = SakId.random(), saksnummer = "sak-opprettet-a", fnr = Fnr.random(), opprettet = fellesOpprettet)
            val andre = inaktivert(sakId = SakId.random(), saksnummer = "sak-opprettet-b", fnr = Fnr.random(), opprettet = fellesOpprettet)

            listOf(første, andre).forEach { varsel ->
                lagreSak(helper, sakId = varsel.sakId, saksnummer = varsel.saksnummer, fnr = varsel.fnr)
                helper.varselPostgresRepo.lagre(varsel)
            }

            helper.varselPostgresRepo.hentVarslerForSakId(første.sakId).single().varselId shouldBe første.varselId
            helper.varselPostgresRepo.hentVarslerForSakId(andre.sakId).single().varselId shouldBe andre.varselId
        }
    }

    @Test
    fun `lagre avviser samme opprettet for samme sak`() {
        withMigratedDb(clock = fixedClockAt(startTid)) { helper ->
            val sakId = SakId.random()
            val fnr = Fnr.random()
            val fellesOpprettet = startTid
            lagreSak(helper, sakId = sakId, saksnummer = "sak-opprettet", fnr = fnr)
            helper.varselPostgresRepo.lagre(
                inaktivert(
                    sakId = sakId,
                    saksnummer = "sak-opprettet",
                    fnr = fnr,
                    opprettet = fellesOpprettet,
                ),
            )

            assertThrows<Throwable> {
                helper.varselPostgresRepo.lagre(
                    inaktivert(
                        sakId = sakId,
                        saksnummer = "sak-opprettet",
                        fnr = fnr,
                        opprettet = fellesOpprettet,
                    ),
                )
            }
        }
    }

    @Test
    fun `lagre avviser flere SkalInaktiveres for samme sak`() {
        withMigratedDb(clock = fixedClockAt(startTid)) { helper ->
            val sakId = SakId.random()
            val fnr = Fnr.random()
            lagreSak(helper, sakId = sakId, saksnummer = "sak-unik-inaktivering", fnr = fnr)
            helper.varselPostgresRepo.lagre(
                skalInaktiveres(
                    sakId = sakId,
                    saksnummer = "sak-unik-inaktivering",
                    fnr = fnr,
                    skalAktiveresTidspunkt = startTid.plusHours(1),
                    opprettet = startTid,
                ),
            )

            assertThrows<Throwable> {
                helper.varselPostgresRepo.lagre(
                    skalInaktiveres(
                        sakId = sakId,
                        saksnummer = "sak-unik-inaktivering",
                        fnr = fnr,
                        skalAktiveresTidspunkt = startTid.plusHours(3),
                        opprettet = startTid.plusMinutes(1),
                    ),
                )
            }
        }
    }

    @Test
    fun `kan lagre SkalInaktiveres og nytt SkalAktiveres i samme transaksjon for samme sak`() {
        withMigratedDb(clock = fixedClockAt(startTid)) { helper ->
            val sakId = SakId.random()
            val fnr = Fnr.random()
            lagreSak(helper, sakId = sakId, saksnummer = "sak-etterfølger", fnr = fnr)
            val aktivtVarsel = aktiv(
                sakId = sakId,
                saksnummer = "sak-etterfølger",
                fnr = fnr,
                skalAktiveresTidspunkt = startTid.plusHours(1),
                opprettet = startTid,
            )
            helper.varselPostgresRepo.lagre(aktivtVarsel)

            val skalInaktiveres = aktivtVarsel.forberedInaktivering(
                skalInaktiveresTidspunkt = startTid.plusHours(2),
                skalInaktiveresBegrunnelse = "mottatt",
            )
            val nyttVarsel = skalAktiveres(
                sakId = sakId,
                saksnummer = "sak-etterfølger",
                fnr = fnr,
                skalAktiveresTidspunkt = startTid.plusDays(1),
                opprettet = startTid.plusMinutes(1),
            )

            helper.sessionFactory.withTransactionContext { tx ->
                helper.varselPostgresRepo.lagre(skalInaktiveres, sessionContext = tx)
                helper.varselPostgresRepo.lagre(nyttVarsel, sessionContext = tx)
            }

            val lagredeVarsler = helper.varselPostgresRepo.hentVarslerForSakId(sakId)
            lagredeVarsler shouldHaveSize 2
            lagredeVarsler.filterIsInstance<Varsel.SkalInaktiveres>().single().varselId shouldBe aktivtVarsel.varselId
            lagredeVarsler.filterIsInstance<Varsel.SkalAktiveres>().single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
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
        opprettet: LocalDateTime = nesteOpprettet(),
    ): Varsel.SkalAktiveres {
        return Varsel.SkalAktiveres(
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
    }

    private fun aktiv(
        sakId: SakId = SakId.random(),
        saksnummer: String,
        varselId: VarselId = VarselId.random(),
        fnr: Fnr = Fnr.random(),
        skalAktiveresTidspunkt: LocalDateTime = 6.januar(2025).atHour(10),
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = nesteOpprettet(),
    ): Varsel.Aktiv {
        return skalAktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            varselId = varselId,
            fnr = fnr,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            clock = clock,
            opprettet = opprettet,
        ).aktiver(skalAktiveresTidspunkt.plusMinutes(5))
    }

    private fun skalInaktiveres(
        sakId: SakId = SakId.random(),
        saksnummer: String,
        varselId: VarselId = VarselId.random(),
        fnr: Fnr = Fnr.random(),
        skalAktiveresTidspunkt: LocalDateTime = 6.januar(2025).atHour(10),
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = nesteOpprettet(),
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
        )
    }

    private fun inaktivert(
        sakId: SakId = SakId.random(),
        saksnummer: String,
        varselId: VarselId = VarselId.random(),
        fnr: Fnr = Fnr.random(),
        skalAktiveresTidspunkt: LocalDateTime = 6.januar(2025).atHour(10),
        clock: Clock = nyClock(),
        opprettet: LocalDateTime = nesteOpprettet(),
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
        return skalInaktiveresVarsel.inaktiver(skalInaktiveresVarsel.skalInaktiveresTidspunkt)
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
