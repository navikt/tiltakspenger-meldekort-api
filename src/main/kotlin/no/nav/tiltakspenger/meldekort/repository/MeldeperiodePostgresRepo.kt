package no.nav.tiltakspenger.meldekort.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.db.prefixColumn
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import tools.jackson.core.type.TypeReference
import java.time.LocalDate

class MeldeperiodePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldeperiodeRepo {
    override fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    insert into meldeperiode (
                        id,
                        kjede_id,
                        versjon,
                        sak_id,
                        opprettet,
                        fra_og_med,
                        til_og_med,
                        maks_antall_dager_for_periode,
                        gir_rett,
                        kan_fylles_ut_fra_og_med
                    ) values (
                        :id,
                        :kjede_id,
                        :versjon,
                        :sak_id,
                        :opprettet,
                        :fra_og_med,
                        :til_og_med,
                        :maks_antall_dager_for_periode,
                        to_jsonb(:gir_rett::jsonb),
                        :kan_fylles_ut_fra_og_med
                    )
                    """,
                    "id" to meldeperiode.id.toString(),
                    "kjede_id" to meldeperiode.kjedeId.toString(),
                    "versjon" to meldeperiode.versjon,
                    "sak_id" to meldeperiode.sakId.toString(),
                    "opprettet" to meldeperiode.opprettet,
                    "fra_og_med" to meldeperiode.periode.fraOgMed,
                    "til_og_med" to meldeperiode.periode.tilOgMed,
                    "maks_antall_dager_for_periode" to meldeperiode.maksAntallDagerForPeriode,
                    "gir_rett" to meldeperiode.girRett.toDbJson(),
                    "kan_fylles_ut_fra_og_med" to meldeperiode.kanFyllesUtFraOgMed,
                ).asUpdate,
            )
        }
    }

    override fun hentForId(id: MeldeperiodeId, sessionContext: SessionContext?): Meldeperiode? {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForId(id, session)
        }
    }

    override fun hentSisteMeldeperiodeForMeldeperiodeKjedeId(
        id: MeldeperiodeKjedeId,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Meldeperiode? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    SELECT DISTINCT ON (s.fnr, mp.kjede_id) mp.*, s.fnr, s.saksnummer
                    FROM meldeperiode mp
                    JOIN sak s ON s.id = mp.sak_id
                    WHERE s.fnr = :fnr
                      AND mp.kjede_id = :id
                    ORDER BY s.fnr, mp.kjede_id, mp.versjon DESC;
                    """.trimIndent(),
                    "id" to id.verdi,
                    "fnr" to fnr.verdi,
                ).map { fromRow(it) }.asSingle,
            )
        }
    }

    override fun hentMeldeperiodeForPeriode(
        periode: Periode,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Meldeperiode? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        SELECT DISTINCT ON (s.fnr, mp.kjede_id) mp.*, s.fnr, s.saksnummer
                        FROM meldeperiode mp
                        JOIN sak s ON s.id = mp.sak_id
                        WHERE mp.fra_og_med = :fra_og_med
                          AND mp.til_og_med = :til_og_med
                          AND s.fnr = :fnr
                        ORDER BY s.fnr, mp.kjede_id, mp.versjon DESC
                    """.trimIndent(),
                    "fra_og_med" to periode.fraOgMed,
                    "til_og_med" to periode.tilOgMed,
                    "fnr" to fnr.verdi,
                ).map { fromRow(it) }.asSingle,
            )
        }
    }

    private fun Map<LocalDate, Boolean>.toDbJson(): String {
        return entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ",",
        ) { (date, value) ->
            "\"${date}\": $value"
        }
    }

    companion object {
        fun hentForId(
            id: MeldeperiodeId,
            session: Session,
        ): Meldeperiode? {
            return session.run(
                sqlQuery(
                    """
                    SELECT mp.*, s.fnr, s.saksnummer
                    FROM meldeperiode mp
                    JOIN sak s ON s.id = mp.sak_id
                    WHERE mp.id = :id
                    """.trimIndent(),
                    "id" to id.toString(),
                ).map { row -> fromRow(row) }.asSingle,
            )
        }

        fun hentForSakId(
            sakId: SakId,
            session: Session,
        ): List<Meldeperiode> {
            return session.run(
                sqlQuery(
                    """
                    select distinct on (fra_og_med)
                        mp.*, s.fnr, s.saksnummer
                    from meldeperiode mp
                    join sak s on s.id = mp.sak_id
                    where mp.sak_id = :sak_id
                    order by mp.fra_og_med, mp.versjon desc
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row -> fromRow(row) }.asList,
            )
        }

        fun fromRow(row: Row, alias: String? = null): Meldeperiode {
            val col = prefixColumn(alias)
            return Meldeperiode(
                id = MeldeperiodeId.fromString(row.string(col("id"))),
                kjedeId = MeldeperiodeKjedeId(row.string(col("kjede_id"))),
                versjon = row.int(col("versjon")),
                sakId = SakId.fromString(row.string(col("sak_id"))),
                fnr = Fnr.fromString(row.string(col("fnr"))),
                opprettet = row.localDateTime(col("opprettet")),
                periode = Periode(
                    fraOgMed = row.localDate(col("fra_og_med")),
                    tilOgMed = row.localDate(col("til_og_med")),
                ),
                maksAntallDagerForPeriode = row.int(col("maks_antall_dager_for_periode")),
                girRett = row.string(col("gir_rett")).fromDbJsonToGirRett(),
                saksnummer = row.string(col("saksnummer")),
                kanFyllesUtFraOgMed = row.localDateTime(col("kan_fylles_ut_fra_og_med")),
            )
        }

        private fun String.fromDbJsonToGirRett(): Map<LocalDate, Boolean> {
            val typeRef = object : TypeReference<Map<LocalDate, Boolean>>() {}
            return objectMapper.readValue(this, typeRef)
        }
    }
}
