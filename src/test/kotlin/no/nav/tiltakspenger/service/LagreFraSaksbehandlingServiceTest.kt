package no.nav.tiltakspenger.service

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.lagMeldekortFraBrukerKommando
import org.junit.jupiter.api.Test
import java.time.LocalTime

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
        (tac.clock as TikkendeKlokke).spolTil(meldeperiodeDto.tilOgMed)

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

    @Test
    fun `Skal kunne fylle ut meldekort i god tid før jul 2025`() {
        val tac = TestApplicationContext()
        val meldeperiodeDto = ObjectMother.meldeperiodeDto(
            periode = Periode(
                fraOgMed = 8.desember(2025),
                tilOgMed = 21.desember(2025),
            ),
        )

        val sakDto = ObjectMother.sakDTO(
            meldeperioder = listOf(
                meldeperiodeDto,
            ),
        )

        tac.lagreFraSaksbehandlingService.lagre(sakDto).getOrFail()

        val sak = tac.sakRepo.hent(SakId.fromString(sakDto.sakId))!!

        val meldekort = tac.meldekortService.hentNesteMeldekortForUtfylling(sak.fnr)!!

        meldekort.meldeperiode.id shouldBe MeldeperiodeId.fromString(
            meldeperiodeDto.id,
        )

        meldekort.meldeperiode.kanFyllesUtFraOgMed shouldBe 17.desember(2025).atTime(LocalTime.of(15, 0))
    }
}
