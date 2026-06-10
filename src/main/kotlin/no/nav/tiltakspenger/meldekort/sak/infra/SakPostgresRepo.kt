package no.nav.tiltakspenger.meldekort.sak.infra

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.arena.infra.tilArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.arena.infra.tilDb
import no.nav.tiltakspenger.meldekort.meldekortvedtak.infra.MeldekortvedtakPostgresRepo
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.meldekort.sak.SakRepo

class SakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : SakRepo {

    override fun oppdaterFnr(
        gammeltFnr: Fnr,
        nyttFnr: Fnr,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    update sak set
                        fnr = :nytt_fnr
                    where fnr = :gammelt_fnr
                    """,
                    "nytt_fnr" to nyttFnr.verdi,
                    "gammelt_fnr" to gammeltFnr.verdi,
                ).asUpdate,
            )
        }
    }

    override fun oppdaterArenaStatus(
        id: SakId,
        arenaStatus: ArenaMeldekortStatus,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    update sak set arena_meldekort_status = :arena_meldekort_status where id = :id
                    """,
                    "id" to id.toString(),
                    "arena_meldekort_status" to arenaStatus.tilDb(),
                ).asUpdate,
            )
        }
    }

    /**
     * Henter med meldeperioder og meldekortvedtak.
     */
    override fun hent(
        id: SakId,
        sessionContext: SessionContext?,
    ): Sak? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select * from sak where id = :id",
                    "id" to id.toString(),
                ).map { row ->
                    fromRow(
                        row = row,
                        medMeldeperioder = true,
                        medMeldekortvedtak = true,
                        session = session,
                    )
                }.asSingle,
            )
        }
    }

    /**
     * Henter ikke med meldeperioder og meldekortvedtak for å unngå unødvendig datalast, ettersom disse ikke trengs i de fleste tilfeller.
     */
    override fun hentSakerUtenArenaStatus(sessionContext: SessionContext?): List<Sak> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select * from sak where arena_meldekort_status = :ukjent_status",
                    "ukjent_status" to ArenaMeldekortStatus.UKJENT.tilDb(),
                ).map { row ->
                    fromRow(
                        row = row,
                        medMeldeperioder = false,
                        medMeldekortvedtak = false,
                        session = session,
                    )
                }.asList,
            )
        }
    }

    companion object {
        private fun fromRow(
            row: Row,
            medMeldeperioder: Boolean,
            medMeldekortvedtak: Boolean,
            session: Session,
        ): Sak {
            val sakId = SakId.fromString(row.string("id"))

            val meldeperioder =
                if (medMeldeperioder) MeldeperiodePostgresRepo.hentSisteMeldeperioderForSakId(sakId, session) else emptyList()
            val meldekortvedtak =
                if (medMeldekortvedtak) MeldekortvedtakPostgresRepo.hentForSakId(sakId, session) else emptyList()

            return Sak(
                id = sakId,
                saksnummer = row.string("saksnummer"),
                fnr = Fnr.fromString(row.string("fnr")),
                meldeperioder = meldeperioder,
                arenaMeldekortStatus = row.string("arena_meldekort_status").tilArenaMeldekortStatus(),
                harSoknadUnderBehandling = row.boolean("har_soknad_under_behandling"),
                kanSendeInnHelgForMeldekort = row.boolean("kan_sende_inn_helg_for_meldekort"),
                meldekortvedtak = meldekortvedtak,
            )
        }
    }
}
