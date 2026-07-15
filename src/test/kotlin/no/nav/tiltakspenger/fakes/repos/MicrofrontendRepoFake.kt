package no.nav.tiltakspenger.fakes.repos

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendFeil
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendMeldekortInfo
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendRepo
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendSak

/**
 * Test-double for in-memory-kontekst (uten ekte Postgres): statusoppdatering er no-op, og lesespørringene returnerer [MicrofrontendFeil.DatabaseFeil] slik at feilhåndteringen i lagene over kan testes uten en ekte databasefeil.
 * De faktiske spørringene dekkes av tester mot ekte Postgres.
 */
class MicrofrontendRepoFake : MicrofrontendRepo {
    override fun oppdaterStatusForMicrofrontend(sakId: SakId, aktiv: Boolean, sessionContext: SessionContext?): Either<MicrofrontendFeil, Unit> {
        // no-op: ingen lesere av microfrontend-status i fakes.
        return Unit.right()
    }

    /** Test-double for [no.nav.tiltakspenger.meldekort.microfrontend.infra.repo.MicrofrontendPostgresRepo.hentSakerHvorMicrofrontendSkalAktiveres]. */
    override fun hentSakerHvorMicrofrontendSkalAktiveres(limit: Int, sessionContext: SessionContext?): Either<MicrofrontendFeil, List<MicrofrontendSak>> {
        // Aggregerte spørringer testes mot ekte Postgres.
        // In-memory returnerer en feil i stedet for å kaste.
        return MicrofrontendFeil.DatabaseFeil(NotImplementedError("Microfrontend-spørringene testes mot ekte Postgres, ikke in-memory.")).left()
    }

    /** Test-double for [no.nav.tiltakspenger.meldekort.microfrontend.infra.repo.MicrofrontendPostgresRepo.hentSakerHvorMicrofrontendSkalInaktiveres]. */
    override fun hentSakerHvorMicrofrontendSkalInaktiveres(limit: Int, sessionContext: SessionContext?): Either<MicrofrontendFeil, List<MicrofrontendSak>> {
        // Aggregerte spørringer testes mot ekte Postgres.
        // In-memory returnerer en feil i stedet for å kaste.
        return MicrofrontendFeil.DatabaseFeil(NotImplementedError("Microfrontend-spørringene testes mot ekte Postgres, ikke in-memory.")).left()
    }

    /** Test-double for [no.nav.tiltakspenger.meldekort.microfrontend.infra.repo.MicrofrontendPostgresRepo.hentMeldekortInfo]. */
    override fun hentMeldekortInfo(fnr: Fnr, sessionContext: SessionContext?): Either<MicrofrontendFeil, MicrofrontendMeldekortInfo> {
        // Spørringen testes mot ekte Postgres.
        // In-memory returnerer en feil slik at route-laget sin feilhåndtering (500) kan dekkes uten en ekte databasefeil.
        return MicrofrontendFeil.DatabaseFeil(NotImplementedError("Microfrontend-spørringene testes mot ekte Postgres, ikke in-memory.")).left()
    }
}
