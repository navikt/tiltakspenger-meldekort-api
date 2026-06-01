package no.nav.tiltakspenger.fakes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.fakes.repos.MeldekortRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldeperiodeRepoFake
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class VarselMeldekortRepoFakeTest {
    private val clock = fixedClockAt(1.mars(2025))
    private val meldekortRepoFake = MeldekortRepoFake(clock)
    private val meldeperiodeRepoFake = MeldeperiodeRepoFake()
    private val varselMeldekortRepoFake = VarselMeldekortRepoFake(meldekortRepoFake, meldeperiodeRepoFake)

    @Test
    fun `returnerer kjede når kanFyllesUtFraOgMed er frem i tid`() {
        val sakId = SakId.random()
        val saksnummer = Math.random().toString()
        val fnr = Fnr.random()
        val meldeperiode = ObjectMother.meldeperiode(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            versjon = 1,
            periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025)),
            opprettet = nå(clock),
            kanFyllesUtFraOgMed = 3.mars(2025).atTime(10, 0),
        )
        val meldekort = ObjectMother.meldekort(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            mottatt = null,
            meldeperiode = meldeperiode,
        )
        meldeperiodeRepoFake.lagre(meldeperiode)
        meldekortRepoFake.lagre(meldekort)

        val resultat = varselMeldekortRepoFake.hentFørsteKjedeSomManglerInnsending(sakId)

        resultat.also {
            requireNotNull(it)
            it.meldeperiodeId shouldBe meldeperiode.id
            it.kjedeId shouldBe meldeperiode.kjedeId
            it.kanFyllesUtFraOgMed shouldBe meldeperiode.kanFyllesUtFraOgMed
        }
    }

    @Test
    fun `returnerer null når tidligere versjon i kjeden har mottatt meldekort`() {
        val sakId = SakId.random()
        val saksnummer = Math.random().toString()
        val fnr = Fnr.random()
        val meldeperiodeV1 = ObjectMother.meldeperiode(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            versjon = 1,
            periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025)),
            opprettet = nå(clock),
        )
        val meldeperiodeV2 = meldeperiodeV1.copy(
            id = MeldeperiodeId.random(),
            versjon = 2,
            opprettet = nå(clock).plusHours(1),
        )
        meldeperiodeRepoFake.lagre(meldeperiodeV1)
        meldeperiodeRepoFake.lagre(meldeperiodeV2)
        meldekortRepoFake.lagre(
            ObjectMother.meldekort(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                mottatt = nå(clock),
                meldeperiode = meldeperiodeV1,
            ),
        )
        meldekortRepoFake.lagre(
            ObjectMother.meldekort(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                mottatt = null,
                meldeperiode = meldeperiodeV2,
            ),
        )

        val resultat = varselMeldekortRepoFake.hentFørsteKjedeSomManglerInnsending(sakId)

        resultat shouldBe null
    }
}
