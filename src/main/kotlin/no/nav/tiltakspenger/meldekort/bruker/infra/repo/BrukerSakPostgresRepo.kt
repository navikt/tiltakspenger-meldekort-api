package no.nav.tiltakspenger.meldekort.bruker.infra.repo

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.arena.infra.tilArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.bruker.BrukerSakRepo
import no.nav.tiltakspenger.meldekort.bruker.SakForBruker

class BrukerSakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BrukerSakRepo {

    /**
     * Henter kun feltene som trengs av brukerflyten — ingen meldeperioder eller meldekortvedtak joines inn.
     * Slik unngår vi at konsumenter feiltolker `emptyList()` som "ingen data finnes".
     */
    override fun hentForBruker(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): SakForBruker? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select
                        fnr,
                        arena_meldekort_status,
                        har_soknad_under_behandling,
                        kan_sende_inn_helg_for_meldekort
                    from sak
                    where fnr = :fnr
                    """,
                    "fnr" to fnr.verdi,
                ).map { row ->
                    SakForBruker(
                        fnr = Fnr.Companion.fromString(row.string("fnr")),
                        arenaMeldekortStatus = row.string("arena_meldekort_status").tilArenaMeldekortStatus(),
                        harSoknadUnderBehandling = row.boolean("har_soknad_under_behandling"),
                        kanSendeInnHelgForMeldekort = row.boolean("kan_sende_inn_helg_for_meldekort"),
                    )
                }.asSingle,
            )
        }
    }
}
