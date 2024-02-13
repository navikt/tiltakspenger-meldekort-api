package no.nav.tiltakspenger.meldekort.api.service

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.clients.utbetaling.UtbetalingClient
import no.nav.tiltakspenger.meldekort.api.db.withTransaction
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortUtenDager
import no.nav.tiltakspenger.meldekort.api.domene.Status
import no.nav.tiltakspenger.meldekort.api.domene.godkjennMeldekort
import no.nav.tiltakspenger.meldekort.api.domene.valider
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagRepo
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagTiltakRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortDagRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortRepo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

private val LOG = KotlinLogging.logger {}

class MeldekortServiceImpl(
    private val meldekortRepo: MeldekortRepo,
    private val meldekortDagRepo: MeldekortDagRepo,
    private val grunnlagRepo: GrunnlagRepo,
    private val grunnlagTiltakRepo: GrunnlagTiltakRepo,
    private val utbetalingClient: UtbetalingClient,
) : MeldekortService {
    override fun genererMeldekort(nyDag: LocalDate) {
        LOG.info { "Generer Meldekort" }
        val grunnlag = grunnlagRepo.hentAktiveGrunnlagForInneværendePeriode()

        grunnlag.map {
            opprettMeldekort(it, nyDag, true)
        }
    }

    override fun hentMeldekort(meldekortId: UUID): Meldekort? {
        LOG.info { "henter meldekort med meldekortId $meldekortId" }
        return meldekortRepo.hentMeldekortMedId(meldekortId)
    }

    private fun opprettMeldekortForGrunnlag(meldekortGrunnlag: MeldekortGrunnlag) {
        opprettMeldekort(meldekortGrunnlag, meldekortGrunnlag.vurderingsperiode.fra, false)
    }

    private fun opprettMeldekort(meldekortGrunnlag: MeldekortGrunnlag, genererFraDato: LocalDate, nyDag: Boolean) {
        val tilDag = if (nyDag) {
            genererFraDato
        } else {
            LocalDate.now()
        }
        when (meldekortGrunnlag.status) {
            Status.AKTIV -> {
                // Skal dette være mindre enn, eller mindre lik ?
                if (meldekortGrunnlag.vurderingsperiode.fra < LocalDate.now()) {
                    val eksisterendeMeldekortPerioder =
                        meldekortRepo.hentPerioderForMeldekortForGrunnlag(meldekortGrunnlag.id)
                    val mandag = finnMandag(genererFraDato)
                    val sisteDagIperioden =
                        finnSisteDagMatte(mandag, minOf(tilDag, meldekortGrunnlag.vurderingsperiode.til))
                    lagMeldekortPerioder(mandag, sisteDagIperioden).mapIndexed { ind, periode ->
                        if (eksisterendeMeldekortPerioder.any { eksisterendePeriode ->
                                eksisterendePeriode.overlapperMed(
                                    periode,
                                )
                            }
                        ) {
                            LOG.info { "Meldekortet overlapper med eksisterende meldekort. Lager ikke nytt" }
                        } else {
                            LOG.info { "Lager nytt meldekort" }
                            val meldekort = Meldekort.Åpent(
                                id = UUID.randomUUID(),
                                løpenr = eksisterendeMeldekortPerioder.size + ind + 1,
                                fom = periode.fra,
                                tom = periode.til,
                                meldekortDager = MeldekortDag.lagIkkeUtfyltPeriode(
                                    periode.fra,
                                    periode.til,
                                ),
                            )
                            meldekortRepo.opprett(meldekortGrunnlag.id, meldekort)
                        }
                    }
                }
            }

            Status.IKKE_AKTIV -> LOG.info { "Fikk et grunnlag som ikke er aktiv. Lager ikke meldekort" }
        }
    }

    override fun hentAlleMeldekortene(grunnlagId: UUID): List<MeldekortUtenDager> {
        LOG.info { "hent meldekort med grunnlagId $grunnlagId" }
        return meldekortRepo.hentMeldekortForGrunnlag(grunnlagId)
    }

    override fun mottaGrunnlag(meldekortGrunnlag: MeldekortGrunnlag) {
        grunnlagRepo.lagre(meldekortGrunnlag)
        opprettMeldekortForGrunnlag(meldekortGrunnlag)
    }

    override fun hentGrunnlagForBehandling(behandlingId: String): MeldekortGrunnlag? {
        return grunnlagRepo.hentForBehandling(behandlingId)
    }

    override fun oppdaterMeldekortDag(meldekortId: UUID, dato: LocalDate, status: MeldekortDagStatus) {
        val grunnlagId = meldekortRepo.hentGrunnlagIdForMeldekort(meldekortId)
        checkNotNull(grunnlagId) { "Fant ikke grunnlag for meldekort med id $meldekortId" }
        val tiltak = grunnlagTiltakRepo.hentFørsteTiltakForGrunnlag(grunnlagId.toString())
        checkNotNull(tiltak) { "Fant ikke tiltak for grunnlag med id $grunnlagId" }
        meldekortDagRepo.oppdater(
            meldekortId = meldekortId,
            tiltakId = tiltak.id,
            dato = dato,
            status = status.name,
        )
    }

    override suspend fun godkjennMeldekort(meldekortId: UUID, saksbehandler: String) {
        val meldekort = meldekortRepo.hentMeldekortMedId(meldekortId)
        checkNotNull(meldekort) { "Vi fant ikke noe meldekort med id $meldekortId" }

        if (meldekort !is Meldekort.Åpent) {
            throw IllegalStateException("Meldekortet er ikke åpent")
        }

        val grunnlagId = meldekortRepo.hentGrunnlagIdForMeldekort(meldekortId)
        checkNotNull(grunnlagId) { "Fant ikke grunnlag for meldekort med id $meldekortId" }

        val grunnlag = grunnlagRepo.hentGrunnlag(grunnlagId)
        checkNotNull(grunnlag) { "Fant ikke grunnlag med id $grunnlagId" }

        check(meldekort.valider()) { "Meldekortet er ikke gyldig" }

        val meldekortDagerMedLøpenummer = meldekort.meldekortDager.map { it.copy(løpenr = meldekort.løpenr) }
        val meldekortBeregning = MeldekortBeregning.beregn(
            meldekortId = meldekortId,
            meldekortDager = meldekortDagRepo.hentInnsendteMeldekortDagerForGrunnlag(grunnlagId) + meldekortDagerMedLøpenummer,
            saksbehandler = saksbehandler,
        )

        utbetalingClient.sendTilUtbetaling(grunnlag.sakId, meldekortBeregning)

        withTransaction { tx ->
            with(meldekort.godkjennMeldekort(saksbehandler)) {
                meldekortRepo.lagreInnsendtMeldekort(this, tx)
            }
        }
    }
}

fun lagMeldekortPerioder(fom: LocalDate, tom: LocalDate): List<Periode> {
    val dager = Periode(fom, tom).tilDager()
    return dager.chunked(14).map {
        Periode(it.first(), it.first().plusDays(13))
    }
}

fun finnMandag(fra: LocalDate): LocalDate {
    return fra.minusDays(fra.dayOfWeek.value.toLong() - 1)
}

fun finnSisteDagMatte(mandag: LocalDate, sisteDag: LocalDate): LocalDate {
    val erIgjenAvPerioden = 14 - mandag.until(sisteDag, ChronoUnit.DAYS) % 14 - 1
    val sisteDagIperioden = sisteDag.plusDays(erIgjenAvPerioden)
    return sisteDagIperioden
}

fun finnSisteDag(mandag: LocalDate, sisteDag: LocalDate): LocalDate {
    check(mandag.dayOfWeek == DayOfWeek.MONDAY) { "Må være mandag" }
    val nesteMandag = mandag.plusDays(14)
    return if (nesteMandag.isAfter(sisteDag)) {
        mandag.plusDays(13)
    } else {
        finnSisteDag(nesteMandag, sisteDag)
    }
}
