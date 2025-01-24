package no.nav.tiltakspenger.meldekort.repository

import com.fasterxml.jackson.core.type.TypeReference
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import java.time.LocalDate

internal class MeldeperiodePostgresRepo(
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
                        :fnr,
                        :opprettet,
                        :fra_og_med,
                        :til_og_med,
                        :maks_antall_dager_for_periode,
                        to_jsonb(:gir_rett::jsonb)
                    )
                    """,
                    "id" to meldeperiode.id,
                    "kjede_id" to meldeperiode.kjedeId,
                    "versjon" to meldeperiode.versjon,
                    "sak_id" to meldeperiode.sakId.toString(),
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

    override fun hentForId(id: String, sessionContext: SessionContext?): Meldeperiode? {
        return sessionFactory.withSession(sessionContext) { session ->
            Companion.hentForId(id, session)
        }
    }

    override fun hentForKjedeId(kjedeId: String, sessionContext: SessionContext?): Meldeperiode? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select * from meldeperiode where kjede_id = :kjede_id",
                    "kjede_id" to kjedeId,
                ).map { row -> fromRow(row) }.asSingle,
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
        internal fun hentForId(
            id: String,
            session: Session,
        ): Meldeperiode? {
            return session.run(
                sqlQuery(
                    "select * from meldeperiode where id = :id",
                    "id" to id,
                ).map { row -> fromRow(row) }.asSingle,
            )
        }

        private fun fromRow(row: Row): Meldeperiode {
            return Meldeperiode(
                id = row.string("id"),
                kjedeId = row.string("kjede_id"),
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
            )
        }

        private fun String.fromDbJsonToGirRett(): Map<LocalDate, Boolean> {
            val typeRef = object : TypeReference<Map<LocalDate, Boolean>>() {}
            return objectMapper.readValue(this, typeRef)
        }
    }
}
