package no.nav.tiltakspenger.service

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.lagMeldekortFraBrukerKommando
import org.junit.jupiter.api.Test

class LagreFraSaksbehandlingServiceTest {

    @Test
    fun `Skal generere meldekort for ny meldeperiode`() {
        val tac = TestApplicationContext()
        val meldeperiodeDto = ObjectMother.meldeperiodeDto()

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

    @Test
    fun `Skal ikke generere nytt meldekort for ny meldeperiode-versjon dersom meldekort allerede var mottatt for meldeperioden`() {
        val tac = TestApplicationContext()
        val meldeperiodeDto = ObjectMother.meldeperiodeDto()

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
