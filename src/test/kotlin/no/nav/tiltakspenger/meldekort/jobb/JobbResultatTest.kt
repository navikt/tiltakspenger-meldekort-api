package no.nav.tiltakspenger.meldekort.jobb

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JobbResultatTest {

    @Test
    fun `tilSamletResultat lar Feilet vinne over UtførteArbeid og IngenArbeid`() {
        listOf(JobbResultat.IngenArbeid, JobbResultat.UtførteArbeid, JobbResultat.Feilet)
            .tilSamletResultat() shouldBe JobbResultat.Feilet
    }

    @Test
    fun `tilSamletResultat lar UtførteArbeid vinne over IngenArbeid`() {
        listOf(JobbResultat.IngenArbeid, JobbResultat.UtførteArbeid)
            .tilSamletResultat() shouldBe JobbResultat.UtførteArbeid
    }

    @Test
    fun `tilSamletResultat gir IngenArbeid når alle del-jobber var uten arbeid`() {
        listOf(JobbResultat.IngenArbeid, JobbResultat.IngenArbeid)
            .tilSamletResultat() shouldBe JobbResultat.IngenArbeid
    }

    @Test
    fun `tilSamletResultat gir IngenArbeid for en tom samling`() {
        emptyList<JobbResultat>().tilSamletResultat() shouldBe JobbResultat.IngenArbeid
    }
}
