package no.nav.tiltakspenger.meldekort.api.service

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.meldekort.api.clients.dokument.DokumentClient
import no.nav.tiltakspenger.meldekort.api.clients.utbetaling.UtbetalingClient
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.domene.UtfallForPeriode
import no.nav.tiltakspenger.meldekort.api.domene.Utfallsperiode
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagRepo
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagTiltakRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortDagRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortRepo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class MeldekortServiceImplTest {
    private lateinit var meldekortService: MeldekortService
    private lateinit var meldekortRepo: MeldekortRepo
    private lateinit var meldekortDagRepo: MeldekortDagRepo
    private lateinit var grunnlagRepo: GrunnlagRepo
    private lateinit var grunnlagTiltakRepo: GrunnlagTiltakRepo
    private lateinit var utbetalingClient: UtbetalingClient
    private lateinit var dokumentClient: DokumentClient
    private val sessionFactory: TestSessionFactory = TestSessionFactory()

    @BeforeEach
    fun setup() {
        meldekortRepo = mockk(relaxed = true)
        meldekortDagRepo = mockk(relaxed = true)
        grunnlagRepo = mockk()
        grunnlagTiltakRepo = mockk(relaxed = true)
        utbetalingClient = mockk()
        dokumentClient = mockk()
        meldekortService =
            MeldekortServiceImpl(
                meldekortRepo,
                meldekortDagRepo,
                grunnlagRepo,
                grunnlagTiltakRepo,
                utbetalingClient,
                dokumentClient,
                sessionFactory,
            )
    }

    @Test
    fun `test at lagPerioder lager riktige meldekortperioder gitt til- og fra-dato`() {
        val fom = LocalDate.of(2021, 11, 1)
        val tom = LocalDate.of(2021, 12, 19)

        val perioder = lagMeldekortPerioder(fom, tom)
        perioder shouldBe
            listOf(
                Periode(fra = LocalDate.of(2021, 11, 1), til = LocalDate.of(2021, 11, 14)),
                Periode(fra = LocalDate.of(2021, 11, 15), til = LocalDate.of(2021, 11, 28)),
                Periode(fra = LocalDate.of(2021, 11, 29), til = LocalDate.of(2021, 12, 12)),
                Periode(fra = LocalDate.of(2021, 12, 13), til = LocalDate.of(2021, 12, 26)),
            )
    }

    @Test
    fun `test at finnMandag finner mandag og at lagIkkeUtfyltPeriode populerer `() {
        val meldekortId = UUID.randomUUID()
        val mandag = finnMandag(LocalDate.of(2021, 11, 1))
        mandag.dayOfWeek shouldBe DayOfWeek.MONDAY

        val tiltak = ObjectMother.tiltak()
        MeldekortDag.lagIkkeUtfyltPeriode(
            meldekortId = meldekortId,
            fom = mandag,
            tom = mandag.plusDays(13),
            utfallsperioder =
            listOf(
                Utfallsperiode(
                    mandag,
                    mandag.plusDays(14),
                    UtfallForPeriode.GIR_RETT_TILTAKSPENGER,
                ),
            ),
            tiltak = tiltak,
        ) shouldBe
            listOf(
                MeldekortDag(mandag, tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(1), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(2), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(3), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(4), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(5), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(6), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(7), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(8), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(9), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(10), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(11), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(12), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(13), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
            )
    }

    @Test
    fun `test at dager uten rett til tiltakspenger blir sperret`() {
        val meldekortId = UUID.randomUUID()
        val utfallsperioder =
            listOf(
                Utfallsperiode(
                    LocalDate.of(2021, 10, 1),
                    LocalDate.of(2021, 11, 2),
                    UtfallForPeriode.GIR_IKKE_RETT_TILTAKSPENGER,
                ),
                Utfallsperiode(
                    LocalDate.of(2021, 11, 3),
                    LocalDate.of(2021, 11, 10),
                    UtfallForPeriode.GIR_RETT_TILTAKSPENGER,
                ),
                Utfallsperiode(
                    LocalDate.of(2021, 11, 11),
                    LocalDate.of(2021, 11, 12),
                    UtfallForPeriode.GIR_IKKE_RETT_TILTAKSPENGER,
                ),
            )
        val mandag = finnMandag(LocalDate.of(2021, 11, 1))
        val tiltak = ObjectMother.tiltak()
        MeldekortDag.lagIkkeUtfyltPeriode(
            meldekortId,
            mandag,
            mandag.plusDays(13),
            utfallsperioder,
            tiltak,
        ) shouldBe
            listOf(
                MeldekortDag(mandag, tiltak, MeldekortDagStatus.SPERRET, meldekortId),
                MeldekortDag(mandag.plusDays(1), tiltak, MeldekortDagStatus.SPERRET, meldekortId),
                MeldekortDag(mandag.plusDays(2), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(3), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(4), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(5), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(6), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(7), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(8), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(9), tiltak, MeldekortDagStatus.IKKE_UTFYLT, meldekortId),
                MeldekortDag(mandag.plusDays(10), tiltak, MeldekortDagStatus.SPERRET, meldekortId),
                MeldekortDag(mandag.plusDays(11), tiltak, MeldekortDagStatus.SPERRET, meldekortId),
                MeldekortDag(
                    mandag.plusDays(12),
                    tiltak,
                    MeldekortDagStatus.SPERRET,
                    meldekortId,
                ), // Utenfor perioden
                MeldekortDag(
                    mandag.plusDays(13),
                    tiltak,
                    MeldekortDagStatus.SPERRET,
                    meldekortId,
                ), // Utenfor perioden
            )
    }

    @Test
    fun `test at dag merket SPERRET ikke skal kunne sendes inn med oppdaterMeldekortDag`() {
        val meldekortId = UUID.randomUUID()
        val dag = LocalDate.of(2021, 11, 1)
        val status = MeldekortDagStatus.SPERRET
        shouldThrow<IllegalStateException> {
            meldekortService.oppdaterMeldekortDag(meldekortId, dag, status)
        }.message shouldBe "Kan ikke sende inn dag med status SPERRET"
    }

    @Test
    fun `test at dag merket IKKE_UTFYLT ikke skal kunne sendes inn med oppdaterMeldekortDag`() {
        val meldekortId = UUID.randomUUID()
        val dag = LocalDate.of(2021, 11, 1)
        val status = MeldekortDagStatus.IKKE_UTFYLT
        shouldThrow<IllegalStateException> {
            meldekortService.oppdaterMeldekortDag(meldekortId, dag, status)
        }.message shouldBe "Kan ikke sende inn dag med status IKKE_UTFYLT"
    }

    @Test
    fun `test at dag med gyldig status skal kunne sendes inn med oppdaterMeldekortDag`() {
        val meldekortId = UUID.randomUUID()
        val dag = LocalDate.of(2021, 11, 1)
        val status = MeldekortDagStatus.DELTATT
        shouldNotThrow<IllegalStateException> {
            meldekortService.oppdaterMeldekortDag(meldekortId, dag, status)
        }
    }

    @Test
    fun `test at finnSisteDag finner riktige toukersperioder fram til til-dato (med rekursjon)`() {
        val til = LocalDate.of(2023, 1, 31) // onsdag
        val fra = LocalDate.of(2023, 1, 4) // onsdag
        val sisteDag = finnSisteDag(finnMandag(fra), til)
        sisteDag shouldBe LocalDate.of(2023, 2, 12)
        val fra2 = LocalDate.of(2023, 1, 11) // onsdag
        val sisteDag2 = finnSisteDag(finnMandag(fra2), til)
        sisteDag2 shouldBe LocalDate.of(2023, 2, 5)
        val til3 = LocalDate.of(2024, 1, 16) // onsdag
        val fra3 = LocalDate.of(2023, 12, 1) // onsdag
        val sisteDag3 = finnSisteDag(finnMandag(fra3), til3)
        sisteDag3 shouldBe LocalDate.of(2024, 1, 21)
    }

    @Test
    fun `test at finnSisteDagMatte finner riktige toukersperioder fram til til-dato (med modulus)`() {
        val til = LocalDate.of(2023, 1, 31) // onsdag
        val fra = LocalDate.of(2023, 1, 4) // onsdag
        val sisteDag = finnSisteDagMatte(finnMandag(fra), til)
        sisteDag shouldBe LocalDate.of(2023, 2, 12)
        val fra2 = LocalDate.of(2023, 1, 11) // onsdag
        val sisteDag2 = finnSisteDagMatte(finnMandag(fra2), til)
        sisteDag2 shouldBe LocalDate.of(2023, 2, 5)
        val til3 = LocalDate.of(2024, 1, 16) // onsdag
        val fra3 = LocalDate.of(2023, 12, 1) // onsdag
        val sisteDag3 = finnSisteDagMatte(finnMandag(fra3), til3)
        sisteDag3 shouldBe LocalDate.of(2024, 1, 21)
    }
}
