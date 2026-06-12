package no.nav.tiltakspenger.meldekort.varsler.infra

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.varsler.KjedeSomManglerInnsending
import no.nav.tiltakspenger.meldekort.varsler.VarselMeldekortRepo

class VarselMeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : VarselMeldekortRepo {

    /**
     * Finner første kjede i saken som "mangler innsending". En kjede mangler innsending dersom:
     * 1. Nyeste versjon har maks_antall_dager_for_periode > 0 (bruker har rett til å fylle ut minst én dag)
     * 2. Det aldri er mottatt et meldekort for noen meldeperiode i kjeden
     */
    override fun hentFørsteKjedeSomManglerInnsending(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): KjedeSomManglerInnsending? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    WITH siste_versjon AS (
                        SELECT DISTINCT ON (kjede_id)
                            id,
                            kjede_id,
                            kan_fylles_ut_fra_og_med,
                            maks_antall_dager_for_periode
                        FROM meldeperiode
                        WHERE sak_id = :sak_id
                        ORDER BY kjede_id, versjon DESC
                    )
                    SELECT
                        mp.id AS meldeperiode_id,
                        mp.kjede_id,
                        mp.kan_fylles_ut_fra_og_med
                    FROM siste_versjon mp
                    WHERE mp.maks_antall_dager_for_periode > 0
                      AND NOT EXISTS (
                          SELECT 1
                          FROM meldekort_bruker mk
                          JOIN meldeperiode mp2 ON mp2.id = mk.meldeperiode_id
                          WHERE mp2.sak_id = :sak_id
                            AND mp2.kjede_id = mp.kjede_id
                            AND mk.mottatt IS NOT NULL
                      )
                      AND NOT EXISTS (
                          SELECT 1
                          FROM meldeperiodebehandling mb
                          WHERE mb.sak_id = :sak_id
                            AND mb.meldeperiode_kjede_id = mp.kjede_id
                      )
                    ORDER BY mp.kan_fylles_ut_fra_og_med
                    LIMIT 1
            """,
                    "sak_id" to sakId.toString(),
                ).map { row ->
                    KjedeSomManglerInnsending(
                        sakId = sakId,
                        meldeperiodeId = MeldeperiodeId.fromString(row.string("meldeperiode_id")),
                        kjedeId = MeldeperiodeKjedeId(row.string("kjede_id")),
                        kanFyllesUtFraOgMed = row.localDateTime("kan_fylles_ut_fra_og_med"),
                    )
                }.asSingle,
            )
        }
    }
}
