package no.nav.tiltakspenger.meldekort.meldekort

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RegistrertMeldekortForKjedeTest {

    private val periode = Periode(6.januar(2025), 19.januar(2025))

    @Test
    fun `digital innsending uten vedtak gir digital tilstand`() {
        val digitalt = ObjectMother.meldekort(periode = periode, mottatt = LocalDateTime.of(2025, 2, 1, 10, 0))

        val registrert = RegistrertMeldekortForKjede(
            meldekortForKjede = MeldekortForKjede(listOf(digitalt)),
            meldekortvedtak = emptyList(),
        ).sisteRegistrerte()!!

        registrert.id shouldBe digitalt.id
        registrert.shouldBeInstanceOf<BrukersMeldekort>()
        registrert.dager shouldBe digitalt.dager
    }

    @Test
    fun `saksbehandler-overstyring av brukers innsending gir fortsatt brukers digitale meldekort som korrigeringsutgangspunkt`() {
        val digitalt = ObjectMother.meldekort(periode = periode, mottatt = LocalDateTime.of(2025, 2, 1, 10, 0))
        // Vedtaket er basert på brukers innsending (brukersMeldekortId satt) og er nyere enn innsendingen.
        val vedtak = ObjectMother.meldekortvedtak(
            meldekort = digitalt,
            opprettet = LocalDateTime.of(2025, 2, 2, 10, 0),
            brukersMeldekortId = digitalt.id,
        )

        val registrert = RegistrertMeldekortForKjede(
            meldekortForKjede = MeldekortForKjede(listOf(digitalt)),
            meldekortvedtak = listOf(vedtak),
        ).sisteRegistrerte()!!

        registrert.id shouldBe digitalt.id
        registrert.shouldBeInstanceOf<BrukersMeldekort>()
        registrert.dager shouldBe digitalt.dager
    }

    @Test
    fun `digital innsending vinner selv om det finnes et papir-only vedtak for kjeden`() {
        val digitalt = ObjectMother.meldekort(periode = periode, mottatt = LocalDateTime.of(2025, 2, 1, 10, 0))
        // Papir-only vedtak (brukersMeldekortId == null), nyere enn brukers innsending.
        val vedtak = ObjectMother.meldekortvedtak(
            meldekort = digitalt,
            opprettet = LocalDateTime.of(2025, 2, 3, 10, 0),
            brukersMeldekortId = null,
        )

        val registrert = RegistrertMeldekortForKjede(
            meldekortForKjede = MeldekortForKjede(listOf(digitalt)),
            meldekortvedtak = listOf(vedtak),
        ).sisteRegistrerte()!!

        registrert.id shouldBe digitalt.id
        registrert.shouldBeInstanceOf<BrukersMeldekort>()
        registrert.dager shouldBe digitalt.dager
    }

    @Test
    fun `papir-only kjede gir vedtak-tilstand med det aapne meldekortet som korrigeringsmaal`() {
        val åpent = ObjectMother.meldekort(periode = periode, mottatt = null)
        val vedtak = ObjectMother.meldekortvedtak(meldekort = åpent, opprettet = LocalDateTime.of(2025, 2, 1, 10, 0))

        val registrert = RegistrertMeldekortForKjede(
            meldekortForKjede = MeldekortForKjede(listOf(åpent)),
            meldekortvedtak = listOf(vedtak),
        ).sisteRegistrerte()!!

        registrert.id shouldBe åpent.id
        registrert.shouldBeInstanceOf<VedtattMeldekort>()
        registrert.dager shouldBe vedtak.meldeperiodebehandlinger.single().dager.map {
            MeldekortDag(dag = it.dato, status = it.status)
        }
    }

    @Test
    fun `verken digital innsending eller vedtak gir null`() {
        val åpent = ObjectMother.meldekort(periode = periode, mottatt = null)

        RegistrertMeldekortForKjede(
            meldekortForKjede = MeldekortForKjede(listOf(åpent)),
            meldekortvedtak = emptyList(),
        ).sisteRegistrerte() shouldBe null
    }
}
