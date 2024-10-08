package no.nav.tiltakspenger.meldekort.api.service

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.api.clients.dokument.DokumentClient
import no.nav.tiltakspenger.meldekort.api.clients.utbetaling.UtbetalingClient
import no.nav.tiltakspenger.meldekort.api.clients.utbetaling.UtbetalingGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortUtenDager
import no.nav.tiltakspenger.meldekort.api.domene.Status
import no.nav.tiltakspenger.meldekort.api.domene.UtbetalingDag
import no.nav.tiltakspenger.meldekort.api.domene.UtbetalingStatus
import no.nav.tiltakspenger.meldekort.api.domene.godkjennMeldekort
import no.nav.tiltakspenger.meldekort.api.domene.valider
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagRepo
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagTiltakRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortDagRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortBeregningDTO
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
    private val dokumentClient: DokumentClient,
    private val sessionFactory: SessionFactory,
) : MeldekortService {
    override fun genererMeldekort(nyDag: LocalDate) {
        LOG.info { "Generer Meldekort for dag: $nyDag" }
        val grunnlag = grunnlagRepo.hentAktiveGrunnlagForInneværendePeriode()

        grunnlag.map {
            opprettMeldekort(it, nyDag, true)
        }
    }

    override fun hentMeldekort(meldekortId: UUID): Meldekort? {
        LOG.info { "henter meldekort med meldekortId $meldekortId" }
        return meldekortRepo.hentMeldekortMedId(meldekortId)
    }

    private fun opprettMeldekortForGrunnlag(
        meldekortGrunnlag: MeldekortGrunnlag,
        transactionContext: TransactionContext? = null,
    ) {
        opprettMeldekort(
            meldekortGrunnlag = meldekortGrunnlag,
            genererFraDato = meldekortGrunnlag.vurderingsperiode.fra,
            nyDag = false,
            transactionContext = transactionContext,
        )
    }

    private fun opprettMeldekort(
        meldekortGrunnlag: MeldekortGrunnlag,
        genererFraDato: LocalDate,
        nyDag: Boolean,
        transactionContext: TransactionContext? = null,
    ) {
        val tilDag =
            if (nyDag) {
                genererFraDato
            } else {
                LocalDate.now()
            }
        when (meldekortGrunnlag.status) {
            Status.AKTIV -> {
                // Skal dette være mindre enn, eller mindre lik ?
                if (meldekortGrunnlag.vurderingsperiode.fra < LocalDate.now()) {
                    val eksisterendeMeldekortPerioder =
                        meldekortRepo.hentPerioderForMeldekortForGrunnlag(meldekortGrunnlag.id, transactionContext)
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
                            val meldekortId = UUID.randomUUID()
                            val meldekort =
                                Meldekort.Åpent(
                                    id = meldekortId,
                                    løpenr = eksisterendeMeldekortPerioder.size + ind + 1,
                                    fom = periode.fra,
                                    tom = periode.til,
                                    meldekortDager =
                                    MeldekortDag.lagIkkeUtfyltPeriode(
                                        meldekortId = meldekortId,
                                        fom = periode.fra,
                                        tom = periode.til,
                                        utfallsperioder = meldekortGrunnlag.utfallsperioder,
                                        tiltak = meldekortGrunnlag.tiltak.single(),
                                    ),
                                )
                            meldekortRepo.opprett(meldekortGrunnlag.id, meldekort, transactionContext)
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
        if (grunnlagRepo.hentGrunnlagForVedtakId(meldekortGrunnlag.vedtakId) != null) {
            LOG.info { "Grunnlag med id ${meldekortGrunnlag.id} finnes allerede. Returnerer OK." }
            return
        }
        sessionFactory.withTransactionContext { tx ->
            grunnlagRepo.lagre(meldekortGrunnlag, tx)
            opprettMeldekortForGrunnlag(meldekortGrunnlag, tx)
        }
    }

    override fun hentGrunnlagForBehandling(behandlingId: String): MeldekortGrunnlag? = grunnlagRepo.hentForBehandling(behandlingId)

    override fun oppdaterMeldekortDag(
        meldekortId: UUID,
        dato: LocalDate,
        status: MeldekortDagStatus,
    ) {
        check(status.kanSendesInnFraMeldekort()) { "Kan ikke sende inn dag med status $status" }
        val grunnlagId = meldekortRepo.hentGrunnlagIdForMeldekort(meldekortId)
        checkNotNull(grunnlagId) { "Fant ikke grunnlag for meldekort med id $meldekortId" }
        val tiltak = grunnlagTiltakRepo.hentFørsteTiltakForGrunnlag(grunnlagId.toString())
        checkNotNull(tiltak) { "Fant ikke tiltak for grunnlag med id $grunnlagId" }

        // overstyrer og setter status til ikke deltatt for dager utenfor tiltaket.
        if (dato.isBefore(tiltak.periode.fra) || dato.isAfter(tiltak.periode.til)) {
            meldekortDagRepo.oppdater(
                meldekortId = meldekortId,
                tiltakId = tiltak.id,
                dato = dato,
                status = MeldekortDagStatus.IKKE_DELTATT.name,
            )
        } else {
            meldekortDagRepo.oppdater(
                meldekortId = meldekortId,
                tiltakId = tiltak.id,
                dato = dato,
                status = status.name,
            )
        }
    }

    override suspend fun hentMeldekortBeregning(meldekortId: UUID): MeldekortBeregningDTO {
        val meldekort = meldekortRepo.hentMeldekortMedId(meldekortId)
        checkNotNull(meldekort) { "Vi fant ikke noe meldekort med id $meldekortId" }

        val grunnlagId = meldekortRepo.hentGrunnlagIdForMeldekort(meldekortId)
        checkNotNull(grunnlagId) { "Fant ikke grunnlag for meldekort med id $meldekortId" }

        val grunnlag = grunnlagRepo.hentGrunnlag(grunnlagId)
        checkNotNull(grunnlag) { "Fant ikke grunnlag med id $grunnlagId" }

        val inneværendeMeldekortDagerMedLøpenummer = meldekort.meldekortDager.map { it.copy(løpenr = meldekort.løpenr) }
        val alleMeldekortDager =
            meldekortDagRepo
                .hentInnsendteMeldekortDagerForGrunnlag(grunnlagId)
                .filterNot { it.meldekortId == meldekortId } +
                inneværendeMeldekortDagerMedLøpenummer
        val utbetalingsDager: List<UtbetalingDag> =
            MeldekortBeregning
                .beregnUtbetalingsDager(
                    meldekortId = meldekortId,
                    meldekortDager =
                    alleMeldekortDager
                        .filter { it.dato >= grunnlag.vurderingsperiode.fra && it.dato <= grunnlag.vurderingsperiode.til }
                        .sortedBy { it.dato },
                    saksbehandler = "saksbehandler",
                ).utbetalingDager
                .filter {
                    it.meldekortId == meldekortId
                }

        val beløpPerDagPerStatus: List<Pair<UtbetalingStatus, Int>> =
            utbetalingsDager.map {
                val satserOgBarnForDato = UtbetalingGrunnlag(
                    sats = 285,
                    satsDelvis = 214,
                    satsBarn = 53,
                    satsBarnDelvis = 40,
                    antallBarn = 0,
                )
                when (it.status) {
                    UtbetalingStatus.IngenUtbetaling -> UtbetalingStatus.IngenUtbetaling to 0
                    UtbetalingStatus.FullUtbetaling ->
                        UtbetalingStatus.FullUtbetaling to
                            satserOgBarnForDato.sats + (satserOgBarnForDato.satsBarn * satserOgBarnForDato.antallBarn)

                    UtbetalingStatus.DelvisUtbetaling ->
                        UtbetalingStatus.DelvisUtbetaling to
                            satserOgBarnForDato.satsDelvis + (satserOgBarnForDato.satsBarnDelvis * satserOgBarnForDato.antallBarn)
                }
            }

        return MeldekortBeregningDTO(
            antallDeltattUtenLønn = meldekort.meldekortDager.count { it.status == MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET },
            antallDeltattMedLønn = meldekort.meldekortDager.count { it.status == MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET },
            antallIkkeDeltatt = meldekort.meldekortDager.count { it.status == MeldekortDagStatus.IKKE_DELTATT },
            antallSykDager = meldekort.meldekortDager.count { it.status == MeldekortDagStatus.FRAVÆR_SYK },
            antallSykBarnDager = meldekort.meldekortDager.count { it.status == MeldekortDagStatus.FRAVÆR_SYKT_BARN },
            antallVelferdGodkjentAvNav = meldekort.meldekortDager.count { it.status == MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV },
            antallVelferdIkkeGodkjentAvNav = meldekort.meldekortDager.count { it.status == MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV },
            antallFullUtbetaling = utbetalingsDager.count { it.status == UtbetalingStatus.FullUtbetaling },
            antallDelvisUtbetaling = utbetalingsDager.count { it.status == UtbetalingStatus.DelvisUtbetaling },
            antallIngenUtbetaling = utbetalingsDager.count { it.status == UtbetalingStatus.IngenUtbetaling },
            sumDelvis =
            beløpPerDagPerStatus
                .filter { it.first == UtbetalingStatus.DelvisUtbetaling }
                .sumOf { it.second },
            sumFull = beløpPerDagPerStatus.filter { it.first == UtbetalingStatus.FullUtbetaling }.sumOf { it.second },
            sumTotal = beløpPerDagPerStatus.sumOf { it.second },
        )
    }

    override suspend fun godkjennMeldekort(
        meldekortId: UUID,
        saksbehandler: String,
    ) {
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

        val inneværendeMeldekortDagerMedLøpenummer = meldekort.meldekortDager.map { it.copy(løpenr = meldekort.løpenr) }
        val alleMeldekortDager =
            meldekortDagRepo.hentInnsendteMeldekortDagerForGrunnlag(grunnlagId) + inneværendeMeldekortDagerMedLøpenummer
        val meldekortBeregning =
            MeldekortBeregning.beregnUtbetalingsDager(
                meldekortId = meldekortId,
                meldekortDager =
                alleMeldekortDager
                    .filter { it.dato >= grunnlag.vurderingsperiode.fra && it.dato <= grunnlag.vurderingsperiode.til }
                    .sortedBy { it.dato },
                saksbehandler = saksbehandler,
            )

        LOG.info { "vi skal sende til utbetaling" }
        utbetalingClient.sendTilUtbetaling(grunnlag.sakId, meldekortBeregning)

        val innsendtMeldekort =
            with(meldekort.godkjennMeldekort(saksbehandler)) {
                LOG.info { "Nå skal vi lagre meldekort" }
                meldekortRepo.lagreInnsendtMeldekort(this)
                this
            }

        try {
            LOG.info { "Nå skal vi sende meldekort til dokument" }
            dokumentClient.sendMeldekortTilDokument(innsendtMeldekort, grunnlag).let {
                LOG.info { "Vi fikk : ${it.journalpostId}" }
                meldekortRepo.lagreJournalPostId(it.journalpostId, innsendtMeldekort.id)
            }
        } catch (e: Exception) {
            LOG.error(e) { "Feil ved sending av meldekort til dokument ${e.message}" }
        }
    }
}

fun lagMeldekortPerioder(
    fom: LocalDate,
    tom: LocalDate,
): List<Periode> {
    val dager = Periode(fom, tom).tilDager()
    return dager.chunked(14).map {
        Periode(it.first(), it.first().plusDays(13))
    }
}

fun finnMandag(fra: LocalDate): LocalDate = fra.minusDays(fra.dayOfWeek.value.toLong() - 1)

fun finnSisteDagMatte(
    mandag: LocalDate,
    sisteDag: LocalDate,
): LocalDate {
    val erIgjenAvPerioden = 14 - mandag.until(sisteDag, ChronoUnit.DAYS) % 14 - 1
    return sisteDag.plusDays(erIgjenAvPerioden)
}

fun finnSisteDag(
    mandag: LocalDate,
    sisteDag: LocalDate,
): LocalDate {
    check(mandag.dayOfWeek == DayOfWeek.MONDAY) { "Må være mandag" }
    val nesteMandag = mandag.plusDays(14)
    return if (nesteMandag.isAfter(sisteDag)) {
        mandag.plusDays(13)
    } else {
        finnSisteDag(nesteMandag, sisteDag)
    }
}
