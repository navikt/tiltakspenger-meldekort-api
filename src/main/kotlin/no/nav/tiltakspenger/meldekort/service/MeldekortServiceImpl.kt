package no.nav.tiltakspenger.meldekort.service

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepoImpl

private val LOG = KotlinLogging.logger {}

class MeldekortServiceImpl(
    val meldekortRepoImpl: MeldekortRepoImpl
) : MeldekortService {
    override suspend fun opprettMeldekort(meldekortDTO: MeldekortDTO) {
        LOG.info { "Start opprett meldekort" }
        meldekortRepoImpl.lagre(meldekortDTO)
    }
}
