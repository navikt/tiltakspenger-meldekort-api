package no.nav.tiltakspenger.service

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.objectmothers.lagMeldekortFraBrukerKommando
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test

class LagreFraSaksbehandlingServiceTest {

    @Test
    fun `Skal generere meldekort for ny meldeperiode`() {
        withTestApplicationContext { tac ->
            val meldeperiodeDto = ObjectMother.meldeperiodeDto(opprettet = nå(tac.clock))

            val sakDto = ObjectMother.sakDTO(
                meldeperioder = listOf(
                    meldeperiodeDto,
                ),
            )

            tac.lagreFraSaksbehandlingService.lagre(sakDto).getOrFail()

            val sak = tac.sakRepo.hent(SakId.fromString(sakDto.sakId))!!

            tac.meldekortService.hentNesteMeldekortForUtfylling(sak.fnr)!!.meldeperiode.id shouldBe MeldeperiodeId.fromString(
                meldeperiodeDto.id,
            )
        }
    }

    @Test
    fun `Skal ikke generere nytt meldekort for ny meldeperiode-versjon dersom meldekort allerede var mottatt for meldeperioden`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldeperiodeDto = ObjectMother.meldeperiodeDto(opprettet = nå(tac.clock))

            val sakDto = ObjectMother.sakDTO(
                meldeperioder = listOf(
                    meldeperiodeDto,
                ),
            )

            tac.lagreFraSaksbehandlingService.lagre(sakDto).getOrFail()

            val sak = tac.sakRepo.hent(SakId.fromString(sakDto.sakId))!!

            val meldekort = tac.meldekortService.hentNesteMeldekortForUtfylling(sak.fnr)!!
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)
            tac.meldekortService.lagreMeldekortFraBruker(lagreKommando)

            val nesteMeldekort = tac.meldekortService.hentNesteMeldekortForUtfylling(sak.fnr)

            nesteMeldekort.shouldBeNull()

            val sakDtoOppdater = sakDto.copy(
                meldeperioder = listOf(
                    meldeperiodeDto.copy(
                        id = MeldeperiodeId.random().toString(),
                        versjon = 2,
                    ),
                ),
            )

            tac.lagreFraSaksbehandlingService.lagre(sakDtoOppdater).getOrFail()

            tac.meldekortService.hentNesteMeldekortForUtfylling(sak.fnr).shouldBeNull()
        }
    }

    @Test
    fun `Skal gjenbruke varselId og deaktivere gammelt meldekort ved revurdering av aktivt meldekort`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldeperiodeDto = ObjectMother.meldeperiodeDto(opprettet = nå(tac.clock))
            val sakDto = ObjectMother.sakDTO(meldeperioder = listOf(meldeperiodeDto))

            tac.lagreFraSaksbehandlingService.lagre(sakDto).getOrFail()

            val sak = tac.sakRepo.hent(SakId.fromString(sakDto.sakId))!!
            val opprinneligMeldekort = tac.meldekortService.hentNesteMeldekortForUtfylling(sak.fnr)!!

            val sakDtoOppdater = sakDto.copy(
                meldeperioder = listOf(
                    meldeperiodeDto.copy(
                        id = MeldeperiodeId.random().toString(),
                        versjon = 2,
                    ),
                ),
            )

            tac.lagreFraSaksbehandlingService.lagre(sakDtoOppdater).getOrFail()

            val meldekortForKjede = tac.meldekortRepo.hentMeldekortForKjedeId(opprinneligMeldekort.meldeperiode.kjedeId, sak.fnr)
            meldekortForKjede.size shouldBe 2

            val gammeltMeldekort = meldekortForKjede.first()
            val nyttMeldekort = meldekortForKjede.last()

            gammeltMeldekort.id shouldBe opprinneligMeldekort.id
            gammeltMeldekort.deaktivert shouldNotBe null
            nyttMeldekort.deaktivert.shouldBeNull()
        }
    }

    @Test
    fun `Skal deaktivere gammelt meldekort uten nytt varselgrunnlag når revurdering fjerner all rett`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val periode = Periode(
                fraOgMed = 6.januar(2025),
                tilOgMed = 19.januar(2025),
            )
            val meldeperiodeDto = ObjectMother.meldeperiodeDto(
                periode = periode,
                opprettet = nå(tac.clock),
            )
            val sakDto = ObjectMother.sakDTO(meldeperioder = listOf(meldeperiodeDto))

            tac.lagreFraSaksbehandlingService.lagre(sakDto).getOrFail()

            val sak = tac.sakRepo.hent(SakId.fromString(sakDto.sakId))!!
            val opprinneligMeldekort = tac.meldekortService.hentNesteMeldekortForUtfylling(sak.fnr)!!

            val sakDtoOppdater = sakDto.copy(
                meldeperioder = listOf(
                    meldeperiodeDto.copy(
                        id = MeldeperiodeId.random().toString(),
                        versjon = 2,
                        girRett = periode.tilDager().associateWith { false },
                        antallDagerForPeriode = 0,
                    ),
                ),
            )

            tac.lagreFraSaksbehandlingService.lagre(sakDtoOppdater).getOrFail()

            tac.meldekortService.hentNesteMeldekortForUtfylling(sak.fnr).shouldBeNull()

            val meldekortForKjede = tac.meldekortRepo.hentMeldekortForKjedeId(opprinneligMeldekort.meldeperiode.kjedeId, sak.fnr)
            meldekortForKjede.size shouldBe 1
            meldekortForKjede.single().also { deaktivertMeldekort ->
                deaktivertMeldekort.id shouldBe opprinneligMeldekort.id
                deaktivertMeldekort.deaktivert shouldNotBe null
            }
        }
    }
}
