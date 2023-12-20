package no.nav.tiltakspenger.meldekort.api.service

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.Status
import no.nav.tiltakspenger.meldekort.api.dto.Meldekort
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortMedTiltak
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortRepo
import java.time.LocalDate
import java.util.UUID

private val LOG = KotlinLogging.logger {}

class MeldekortServiceImpl(
    private val meldekortRepo: MeldekortRepo,
    private val grunnlagRepo: GrunnlagRepo,
) : MeldekortService {
    override fun genererMeldekort(fraDato: LocalDate) {
        LOG.info { "Generer Meldekort" }
        val grunnlag = grunnlagRepo.hentAktiveGrunnlagForInneværendePeriode()

        grunnlag.map {
            opprettMeldekort(it, fraDato)
        }
    }

    private fun opprettMeldekortForGrunnlag(meldekortGrunnlag: MeldekortGrunnlag) {
        opprettMeldekort(meldekortGrunnlag, meldekortGrunnlag.vurderingsperiode.fra)
    }

    private fun opprettMeldekort(meldekortGrunnlag: MeldekortGrunnlag, fraDato: LocalDate) {
        when (meldekortGrunnlag.status) {
            Status.AKTIV -> {
                if (meldekortGrunnlag.vurderingsperiode.fra < LocalDate.now()) {
                    val eksisterendeMeldekortPerioder = meldekortRepo.hentPerioderForMeldekortForGrunnlag(meldekortGrunnlag.id)

                    val mandag = finnMandag(fraDato)
                    lagMeldekortPerioder(mandag, mandag.plusDays(13)).map {
                        if (eksisterendeMeldekortPerioder.any { eksisterendePeriode -> eksisterendePeriode.overlapperMed(it) }) {
                            LOG.info { "Meldekortet overlapper med eksisterende meldekort. Lager ikke nytt" }
                        } else {
                            LOG.info { "Lager nytt meldekort" }
                            val meldekort = Meldekort.Åpent(
                                id = UUID.randomUUID(),
                                fom = it.fra,
                                tom = it.til,
                                meldekortDager = MeldekortDag.lagIkkeUtfyltPeriode(
                                    it.fra,
                                    it.til,
                                ),
                            )
                            meldekortRepo.lagre(meldekortGrunnlag.id, meldekort)
                        }
                    }
                }
            }
            Status.IKKE_AKTIV -> LOG.info { "Fikk et grunnlag som ikke er aktiv. Lager ikke meldekort" }
        }
    }

    //                        { meldekort ->
//                            // hvis vi har det, så skal vi ikke lage et nytt
//                            Meldekort.Åpent(
//                                id = UUID.randomUUID(),
//                                fom = it.fra,
//                                tom = it.til,
//                                meldekortDager = MeldekortDag.lagIkkeUtfyltPeriode(
//                                    it.fra,
//                                    it.til,
//                                    meldekortGrunnlagDTO.tiltak.first()
//                                ),
//                            )
//                    }
//                    meldekortRepo.lagre(meldekort)
    override fun hentMeldekort(id: String): MeldekortMedTiltak? {
        LOG.info { "hent meldekort med meldekortIdent $id" }
        return meldekortRepo.hent(id)
    }

    override fun hentAlleMeldekortene(behandlingId: String): List<MeldekortMedTiltak> {
        TODO("Not yet implemented")
    }

    override fun mottaGrunnlag(meldekortGrunnlag: MeldekortGrunnlag) {
        grunnlagRepo.lagre(meldekortGrunnlag)
        opprettMeldekortForGrunnlag(meldekortGrunnlag)
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
