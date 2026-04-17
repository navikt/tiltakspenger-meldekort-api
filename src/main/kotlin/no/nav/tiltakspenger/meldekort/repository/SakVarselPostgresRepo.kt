package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.Sak
import java.time.LocalDateTime

class SakVarselPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : SakVarselRepo {

    override fun flaggForVarselvurdering(
        sakId: SakId,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "UPDATE sak SET skal_vurdere_varsel = true WHERE id = :id",
                    "id" to sakId.toString(),
                ).asUpdate,
            )
        }
    }

    override fun hentSakerSomSkalVurdereVarsel(
        limit: Int,
        sessionContext: SessionContext?,
    ): List<Sak> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT * FROM sak
                    WHERE skal_vurdere_varsel = true
                    ORDER BY id
                    LIMIT :limit
                    """,
                    "limit" to limit,
                ).map { row ->
                    val sakId = SakId.fromString(row.string("id"))
                    val meldeperioder = MeldeperiodePostgresRepo.hentForSakId(sakId, session)
                    Sak(
                        id = sakId,
                        saksnummer = row.string("saksnummer"),
                        fnr = Fnr.fromString(row.string("fnr")),
                        meldeperioder = meldeperioder,
                        arenaMeldekortStatus = row.string("arena_meldekort_status").tilArenaMeldekortStatus(),
                        harSoknadUnderBehandling = row.boolean("har_soknad_under_behandling"),
                        kanSendeInnHelgForMeldekort = row.boolean("kan_sende_inn_helg_for_meldekort"),
                    )
                }.asList,
            )
        }
    }

    override fun markerVarselVurdert(
        sakId: SakId,
        vurdertTidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    UPDATE sak SET
                        skal_vurdere_varsel = false,
                        sist_vurdert_varsel = :vurdert_tidspunkt
                    WHERE id = :id
                    """,
                    "id" to sakId.toString(),
                    "vurdert_tidspunkt" to vurdertTidspunkt,
                ).asUpdate,
            )
        }
    }
}
