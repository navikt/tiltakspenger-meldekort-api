package no.nav.tiltakspenger.meldekort.repository

import com.fasterxml.jackson.core.type.TypeReference
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
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
                        saksnummer,
                        fnr,
                        opprettet,
                        fra_og_med,
                        til_og_med,
                        maks_antall_dager_for_periode,
                        gir_rett
                    ) values (
                        :id,
                        :kjede_id,
                        :versjon,
                        :sak_id,
                        :saksnummer,
                        :fnr,
                        :opprettet,
                        :fra_og_med,
                        :til_og_med,
                        :maks_antall_dager_for_periode,
                        to_jsonb(:gir_rett::jsonb)
                    )
                    """,
                    "id" to meldeperiode.id.toString(),
                    "kjede_id" to meldeperiode.kjedeId.toString(),
                    "versjon" to meldeperiode.versjon,
                    "sak_id" to meldeperiode.sakId.toString(),
                    "saksnummer" to meldeperiode.saksnummer,
                    "fnr" to meldeperiode.fnr.verdi,
                    "opprettet" to meldeperiode.opprettet,
                    "fra_og_med" to meldeperiode.periode.fraOgMed,
                    "til_og_med" to meldeperiode.periode.tilOgMed,
                    "maks_antall_dager_for_periode" to meldeperiode.maksAntallDagerForPeriode,
                    "gir_rett" to meldeperiode.girRett.toDbJson(),
                ).asUpdate,
            )
        }
    }

    override fun hentForId(id: MeldeperiodeId, sessionContext: SessionContext?): Meldeperiode? {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForId(id, session)
        }
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, sessionContext: SessionContext?) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                queryOf(
                    """update meldeperiode set fnr = :nytt_fnr where fnr = :gammelt_fnr""",
                    mapOf(
                        "nytt_fnr" to nyttFnr.verdi,
                        "gammelt_fnr" to gammeltFnr.verdi,
                    ),
                ).asUpdate,
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
                    "select * from meldeperiode where id = :id",
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
                        * from meldeperiode where sak_id = :sak_id
                    order by fra_og_med, versjon desc
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row -> fromRow(row) }.asList,
            )
        }

        private fun fromRow(row: Row): Meldeperiode {
            return Meldeperiode(
                id = MeldeperiodeId.fromString(row.string("id")),
                kjedeId = MeldeperiodeKjedeId(row.string("kjede_id")),
                versjon = row.int("versjon"),
                sakId = SakId.fromString(row.string("sak_id")),
                fnr = Fnr.fromString(row.string("fnr")),
                opprettet = row.localDateTime("opprettet"),
                periode = Periode(
                    fraOgMed = row.localDate("fra_og_med"),
                    tilOgMed = row.localDate("til_og_med"),
                ),
                maksAntallDagerForPeriode = row.int("maks_antall_dager_for_periode"),
                girRett = row.string("gir_rett").fromDbJsonToGirRett(),
                saksnummer = row.string("saksnummer"),
            )
        }

        private fun String.fromDbJsonToGirRett(): Map<LocalDate, Boolean> {
            val typeRef = object : TypeReference<Map<LocalDate, Boolean>>() {}
            return objectMapper.readValue(this, typeRef)
        }
    }
}
