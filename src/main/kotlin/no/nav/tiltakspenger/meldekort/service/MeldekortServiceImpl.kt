package no.nav.tiltakspenger.meldekort.service

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo

private val LOG = KotlinLogging.logger {}

class MeldekortServiceImpl(
    val meldekortRepo: MeldekortRepo
) : MeldekortService {
    override suspend fun opprettMeldekort(meldekortDTO: MeldekortDTO) {
        LOG.info { "Start opprett meldekort" }
        meldekortRepo.lagre(meldekortDTO)
    }

    override suspend fun hentMeldekort(meldekortIdent: String) {
        LOG.info{ "hent meldekort med meldekortIdent $meldekortIdent" }
        meldekortRepo.hent(meldekortIdent)
    }

    override suspend fun hentAlleMeldekortene(sakId: String) {
        TODO("Not yet implemented")
    }
}
