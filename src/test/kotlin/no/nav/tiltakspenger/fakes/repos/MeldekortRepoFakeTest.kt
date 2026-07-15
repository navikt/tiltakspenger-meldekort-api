package no.nav.tiltakspenger.fakes.repos

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

/**
 * Verifiserer at [MeldekortRepoFake] speiler ekskluderingen i [no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortPostgresRepo]: Et uinnsendt meldekort regnes ikke som "mangler innsending" dersom det finnes et meldekortvedtak (f.eks. papirmeldekort) for kjeden.
 */
class MeldekortRepoFakeTest {
    private val clock = fixedClockAt(1.mars(2025))

    @Test
    fun `hentNesteMeldekortTilUtfylling ekskluderer kjede med meldekortvedtak`() {
        val meldekortvedtakRepoFake = MeldekortvedtakRepoFake()
        val repo = MeldekortRepoFake(clock, meldekortvedtakRepoFake)
        val fnr = Fnr.random()
        val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

        val medVedtak = ObjectMother.meldekort(fnr = fnr, mottatt = null, periode = førstePeriode)
        val utenVedtak = ObjectMother.meldekort(
            fnr = fnr,
            sakId = medVedtak.sakId,
            saksnummer = medVedtak.saksnummer,
            mottatt = null,
            periode = førstePeriode.plus14Dager(),
        )
        repo.lagre(medVedtak)
        repo.lagre(utenVedtak)
        meldekortvedtakRepoFake.lagre(ObjectMother.meldekortvedtak(meldekort = medVedtak), null)

        repo.hentNesteMeldekortTilUtfylling(fnr) shouldBe utenVedtak
    }

    @Test
    fun `hentAlleMeldekortKlarTilInnsending ekskluderer kjede med meldekortvedtak`() {
        val meldekortvedtakRepoFake = MeldekortvedtakRepoFake()
        val repo = MeldekortRepoFake(clock, meldekortvedtakRepoFake)
        val fnr = Fnr.random()
        val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

        val medVedtak = ObjectMother.meldekort(fnr = fnr, mottatt = null, periode = førstePeriode)
        val utenVedtak = ObjectMother.meldekort(
            fnr = fnr,
            sakId = medVedtak.sakId,
            saksnummer = medVedtak.saksnummer,
            mottatt = null,
            periode = førstePeriode.plus14Dager(),
        )
        repo.lagre(medVedtak)
        repo.lagre(utenVedtak)
        meldekortvedtakRepoFake.lagre(ObjectMother.meldekortvedtak(meldekort = medVedtak), null)

        repo.hentAlleMeldekortKlarTilInnsending(fnr).map { it.id } shouldBe listOf(utenVedtak.id)
    }
}
