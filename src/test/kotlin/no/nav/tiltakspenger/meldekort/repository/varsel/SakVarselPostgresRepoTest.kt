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
import no.nav.tiltakspenger.meldekort.repository.OptimistiskLåsFeil
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SakVarselPostgresRepoTest {
    @Test
    fun `hentSakerSomSkalVurdereVarsel returnerer bare flaggede saker prioritert på sist_flagget_tidspunkt og begrenset av limit`() {
        withMigratedDb { helper ->
            // Tre saker, alle flagget ved migreringen (sist_flagget_tidspunkt = NULL).
            // Med ORDER BY sist_flagget_tidspunkt NULLS FIRST, id forventer vi at de plukkes i id-rekkefølge.
            val førsteSak = sakMedMeldeperiode()
            val andreSak = sakMedMeldeperiode()
            val tredjeSak = sakMedMeldeperiode()
            lagreSak(helper, førsteSak, andreSak, tredjeSak)
            helper.sakVarselPostgresRepo.markerVarselVurdert(
                sakId = andreSak.id,
                vurdertTidspunkt = nå(fixedClock),
                sistFlaggetTidspunktVedLesing = null,
            )

            val resultat = helper.sakVarselPostgresRepo.hentSakerSomSkalVurdereVarsel(limit = 2)
            val forventet = listOf(førsteSak, tredjeSak)
                .sortedBy { it.id.toString() }
                .take(2)

            resultat.map { it.sakId } shouldContainExactly forventet.map { it.id }
            // Sjekk at saksnummer/fnr returneres slik at VurderVarselService ikke trenger
            // å laste hele Sak-aggregatet.
            resultat.map { it.saksnummer } shouldContainExactly forventet.map { it.saksnummer }
            resultat.map { it.fnr } shouldContainExactly forventet.map { it.fnr }
        }
    }

    @Test
    fun `hentSakerSomSkalVurdereVarsel prioriterer first-flagged-first-served`() {
        withMigratedDb { helper ->
            val gammelSak = sakMedMeldeperiode()
            val nySak = sakMedMeldeperiode()
            lagreSak(helper, gammelSak, nySak)

            // Marker begge som vurdert, og flagg dem på nytt i en bestemt rekkefølge slik at
            // gammelSak får et tidligere sist_flagget_tidspunkt enn nySak.
            helper.sakVarselPostgresRepo.markerVarselVurdert(gammelSak.id, nå(fixedClock), null)
            helper.sakVarselPostgresRepo.markerVarselVurdert(nySak.id, nå(fixedClock), null)
            helper.sakVarselPostgresRepo.flaggForVarselvurdering(gammelSak.id)
            helper.sakVarselPostgresRepo.flaggForVarselvurdering(nySak.id)

            val resultat = helper.sakVarselPostgresRepo.hentSakerSomSkalVurdereVarsel()

            resultat.map { it.sakId } shouldContainExactly listOf(gammelSak.id, nySak.id)
        }
    }

    @Test
    fun `markerVarselVurdert setter flagget til false og lagrer tidspunkt`() {
        withMigratedDb { helper ->
            val sak = sakMedMeldeperiode()
            val vurdertTidspunkt = 6.januar(2025).atHour(10)
            lagreSak(helper, sak)

            helper.sakVarselPostgresRepo.markerVarselVurdert(
                sakId = sak.id,
                vurdertTidspunkt = vurdertTidspunkt,
                sistFlaggetTidspunktVedLesing = null,
            )
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
            helper.sakVarselPostgresRepo.markerVarselVurdert(
                sakId = sak.id,
                vurdertTidspunkt = vurdertTidspunkt,
                sistFlaggetTidspunktVedLesing = null,
            )

            helper.sakVarselPostgresRepo.flaggForVarselvurdering(sak.id)

            helper.sakVarselPostgresRepo.hentSakerSomSkalVurdereVarsel().map { it.sakId } shouldContainExactly listOf(sak.id)
            hentSakVarselstatus(helper, sak.id) shouldBe SakVarselstatus(
                skalVurdereVarsel = true,
                sistVurdertVarsel = vurdertTidspunkt,
            )
        }
    }

    @Test
    fun `markerVarselVurdert kaster OptimistiskLåsFeil når sist_flagget_tidspunkt er endret siden lesing`() {
        withMigratedDb { helper ->
            val sak = sakMedMeldeperiode()
            lagreSak(helper, sak)

            // Simuler at jobben leste saken før noe ble flagget (sist_flagget_tidspunkt = null)
            // og at et meldekort kom inn i mellomtiden og oppdaterte tidspunktet.
            helper.sakVarselPostgresRepo.flaggForVarselvurdering(sak.id)

            io.kotest.assertions.throwables.shouldThrow<OptimistiskLåsFeil> {
                helper.sakVarselPostgresRepo.markerVarselVurdert(
                    sakId = sak.id,
                    vurdertTidspunkt = nå(fixedClock),
                    sistFlaggetTidspunktVedLesing = null,
                )
            }

            // Flagget forblir true slik at saken plukkes opp på nytt.
            helper.sakVarselPostgresRepo.hentSakerSomSkalVurdereVarsel()
                .map { it.sakId } shouldContainExactly listOf(sak.id)
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
