package no.nav.tiltakspenger.meldekort.api.service

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.dto.Meldekort
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortMedTiltak
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.api.routes.MeldekortGrunnlagDTO
import no.nav.tiltakspenger.meldekort.api.routes.StatusDTO

private val LOG = KotlinLogging.logger {}

class MeldekortServiceImpl(
    private val meldekortRepo: MeldekortRepo,
    private val grunnlagRepo: GrunnlagRepo,
) : MeldekortService {
    override fun opprettMeldekort(meldekort: Meldekort.Registrert) {
        LOG.info { "Start opprett meldekort" }
        meldekortRepo.lagre(meldekort)
    }

    override fun hentMeldekort(id: String): MeldekortMedTiltak? {
        LOG.info { "hent meldekort med meldekortIdent $id" }
        return meldekortRepo.hent(id)
    }

    override fun hentAlleMeldekortene(behandlingId: String): List<MeldekortMedTiltak> {
        TODO("Not yet implemented")
    }

    override fun mottaGrunnlag(meldekortGrunnlagDTO: MeldekortGrunnlagDTO) {
        grunnlagRepo.lagre(meldekortGrunnlagDTO)
        when (meldekortGrunnlagDTO.status) {
            StatusDTO.AKTIV -> opprettMeldekort(
                Meldekort.Registrert(
                    id = "",
                    fom = meldekortGrunnlagDTO.vurderingsperiode.fra,
                    tom = meldekortGrunnlagDTO.vurderingsperiode.til,
                    meldekortUke1 = listOf(),
                    meldekortUke2 = listOf(),
                ),
            )
            StatusDTO.IKKE_AKTIV -> LOG.info { "Fikk et grunnlag som ikke er aktiv. Lager ikke meldekort" }
        }
    }
}
