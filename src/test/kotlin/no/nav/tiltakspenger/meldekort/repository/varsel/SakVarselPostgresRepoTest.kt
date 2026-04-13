package no.nav.tiltakspenger.meldekort.repository.varsel

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SakVarselPostgresRepoTest {
    @Test
    fun `hentSakerSomSkalVurdereVarsel returnerer bare flaggede saker sortert på id og begrenset av limit`() {
        withMigratedDb { helper ->
            val førsteSak = sakMedMeldeperiode()
            val andreSak = sakMedMeldeperiode()
            val tredjeSak = sakMedMeldeperiode()
            lagreSak(helper, førsteSak, andreSak, tredjeSak)
            helper.sakVarselPostgresRepo.markerVarselVurdert(andreSak.id, nå(fixedClock))

            val resultat = helper.sakVarselPostgresRepo.hentSakerSomSkalVurdereVarsel(limit = 2)
            val forventet = listOf(førsteSak, tredjeSak)
                .sortedBy { it.id.toString() }
                .take(2)

            resultat.map { it.id } shouldContainExactly forventet.map { it.id }
            resultat.map { it.meldeperioder.single().id } shouldContainExactly forventet.map { it.meldeperioder.single().id }
        }
    }

    @Test
    fun `markerVarselVurdert setter flagget til false og lagrer tidspunkt`() {
        withMigratedDb { helper ->
            val sak = sakMedMeldeperiode()
            val vurdertTidspunkt = 6.januar(2025).atHour(10)
            lagreSak(helper, sak)

            helper.sakVarselPostgresRepo.markerVarselVurdert(sak.id, vurdertTidspunkt)

            helper.sakVarselPostgresRepo.hentSakerSomSkalVurdereVarsel().shouldBeEmpty()
            hentSakVarselstatus(helper, sak.id) shouldBe SakVarselstatus(
                skalVurdereVarsel = false,
                sistVurdertVarsel = vurdertTidspunkt,
            )
        }
    }

    @Test
    fun `flaggForVarselvurdering setter flagget tilbake til true`() {
        withMigratedDb { helper ->
            val sak = sakMedMeldeperiode()
            val vurdertTidspunkt = 6.januar(2025).atHour(10)
            lagreSak(helper, sak)
            helper.sakVarselPostgresRepo.markerVarselVurdert(sak.id, vurdertTidspunkt)

            helper.sakVarselPostgresRepo.flaggForVarselvurdering(sak.id)

            helper.sakVarselPostgresRepo.hentSakerSomSkalVurdereVarsel().map { it.id } shouldContainExactly listOf(sak.id)
            hentSakVarselstatus(helper, sak.id) shouldBe SakVarselstatus(
                skalVurdereVarsel = true,
                sistVurdertVarsel = vurdertTidspunkt,
            )
        }
    }

    private fun sakMedMeldeperiode(
        sakId: SakId = SakId.random(),
        saksnummer: String = Math.random().toString(),
        fnr: Fnr = Fnr.random(),
    ): Sak {
        val meldeperiode = ObjectMother.meldeperiode(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = nå(fixedClock),
        )
        return ObjectMother.sak(
            id = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            meldeperioder = listOf(meldeperiode),
        )
    }

    private fun lagreSak(helper: TestDataHelper, vararg saker: Sak) {
        saker.forEach { sak ->
            sak.meldeperioder.forEach { meldeperiode -> helper.meldeperiodeRepo.lagre(meldeperiode) }
            helper.sakPostgresRepo.lagre(sak)
        }
    }

    private fun hentSakVarselstatus(helper: TestDataHelper, sakId: SakId): SakVarselstatus {
        return helper.sessionFactory.withSession(null) { session ->
            session.run(
                sqlQuery(
                    """
                    select skal_vurdere_varsel, sist_vurdert_varsel
                    from sak
                    where id = :id
                    """.trimIndent(),
                    "id" to sakId.toString(),
                ).map { row ->
                    SakVarselstatus(
                        skalVurdereVarsel = row.boolean("skal_vurdere_varsel"),
                        sistVurdertVarsel = row.localDateTimeOrNull("sist_vurdert_varsel"),
                    )
                }.asSingle,
            )!!
        }
    }

    private data class SakVarselstatus(
        val skalVurdereVarsel: Boolean,
        val sistVurdertVarsel: LocalDateTime?,
    )
}

private fun LocalDate.atHour(time: Int): LocalDateTime = this.atTime(time, 0)
