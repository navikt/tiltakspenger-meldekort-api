package no.nav.tiltakspenger.meldekort.api.service

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortRepo
import no.nav.tiltakspenger.meldekort.api.routes.MeldekortGrunnlagDTO

private val LOG = KotlinLogging.logger {}

class MeldekortServiceImpl(
    private val meldekortRepo: MeldekortRepo,
    private val grunnlagRepo: GrunnlagRepo,
) : MeldekortService {
    override suspend fun opprettMeldekort(meldekortDTO: MeldekortDTO) {
        LOG.info { "Start opprett meldekort" }
        meldekortRepo.lagre(meldekortDTO)
    }

    override suspend fun hentMeldekort(meldekortIdent: String) {
        LOG.info { "hent meldekort med meldekortIdent $meldekortIdent" }
        meldekortRepo.hent(meldekortIdent)
    }

    override suspend fun hentAlleMeldekortene(sakId: String) {
        TODO("Not yet implemented")
    }

    override fun mottaGrunnlag(meldekortGrunnlagDTO: MeldekortGrunnlagDTO) {
        grunnlagRepo.lagre(meldekortGrunnlagDTO)
    }

}
