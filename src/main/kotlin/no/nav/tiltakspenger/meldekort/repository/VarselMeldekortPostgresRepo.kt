package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.varsler.KjedeSomManglerInnsending

class VarselMeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : VarselMeldekortRepo {

    /**
     * For hver kjede i saken, finn nyeste meldeperiode-versjon.
     * En kjede "mangler innsending" dersom:
     * 1. Nyeste versjon har maks_antall_dager_for_periode > 0 (bruker har rett til å fylle ut minst én dag)
     * 2. Det ikke finnes et meldekort knyttet til denne nyeste versjonen som er innsendt (mottatt IS NOT NULL) og ikke deaktivert
     */
    override fun hentKjederSomManglerInnsending(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): List<KjedeSomManglerInnsending> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    WITH nyeste_meldeperiode_per_kjede AS (
                        SELECT DISTINCT ON (mp.kjede_id)
                            mp.id AS meldeperiode_id,
                            mp.kjede_id,
                            mp.versjon,
                            mp.sak_id,
                            mp.maks_antall_dager_for_periode,
                            mp.kan_fylles_ut_fra_og_med
                        FROM meldeperiode mp
                        WHERE mp.sak_id = :sak_id
                        ORDER BY mp.kjede_id, mp.versjon DESC
                    )
                    SELECT
                        nm.sak_id,
                        nm.meldeperiode_id,
                        nm.kjede_id,
                        nm.versjon,
                        nm.kan_fylles_ut_fra_og_med
                    FROM nyeste_meldeperiode_per_kjede nm
                    WHERE nm.maks_antall_dager_for_periode > 0
                    AND NOT EXISTS (
                        SELECT 1
                        FROM meldekort_bruker mk
                        WHERE mk.meldeperiode_id = nm.meldeperiode_id
                          AND mk.mottatt IS NOT NULL
                          AND mk.deaktivert IS NULL
                    )
                    ORDER BY nm.kan_fylles_ut_fra_og_med
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row ->
                    KjedeSomManglerInnsending(
                        sakId = SakId.fromString(row.string("sak_id")),
                        meldeperiodeId = MeldeperiodeId.fromString(row.string("meldeperiode_id")),
                        kjedeId = MeldeperiodeKjedeId(row.string("kjede_id")),
                        nyesteVersjon = row.int("versjon"),
                        kanFyllesUtFraOgMed = row.localDateTime("kan_fylles_ut_fra_og_med"),
                    )
                }.asList,
            )
        }
    }
}
