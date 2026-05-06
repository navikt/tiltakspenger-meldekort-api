package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.varsler.BeskjedVarsel

class BeskjedVarselPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : BeskjedVarselRepo {
    override fun lagre(
        beskjedVarsel: BeskjedVarsel,
        sendingsmetadata: String,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    insert into beskjed_varsel (
                        varsel_id,
                        sak_id,
                        sendingsmetadata,
                        opprettet
                    ) values (
                        :varsel_id,
                        :sak_id,
                        :sendingsmetadata,
                        :opprettet
                    )
                    """,
                    "varsel_id" to beskjedVarsel.varselId.toString(),
                    "sak_id" to beskjedVarsel.sakId.toString(),
                    "sendingsmetadata" to sendingsmetadata,
                    "opprettet" to beskjedVarsel.opprettet,
                ).asUpdate,
            )
            beskjedVarsel.meldeperioder.forEach { meldeperiode ->
                session.run(
                    sqlQuery(
                        """
                        insert into beskjed_varsel_meldeperiode (
                            varsel_id,
                            sak_id,
                            meldeperiode_id,
                            kjede_id,
                            versjon,
                            siste_innsendte_versjon,
                            endringsmetadata
                        ) values (
                            :varsel_id,
                            :sak_id,
                            :meldeperiode_id,
                            :kjede_id,
                            :versjon,
                            :siste_innsendte_versjon,
                            to_jsonb(:endringsmetadata::jsonb)
                        )
                        """,
                        "varsel_id" to beskjedVarsel.varselId.toString(),
                        "sak_id" to meldeperiode.sakId.toString(),
                        "meldeperiode_id" to meldeperiode.meldeperiodeId.toString(),
                        "kjede_id" to meldeperiode.kjedeId.toString(),
                        "versjon" to meldeperiode.versjon,
                        "siste_innsendte_versjon" to meldeperiode.sisteInnsendteVersjon,
                        "endringsmetadata" to objectMapper.writeValueAsString(meldeperiode.endring),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hentAntallBeskjederForSak(sakId: SakId, sessionContext: SessionContext?): Int {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select count(*) as antall
                    from beskjed_varsel
                    where sak_id = :sak_id
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row -> row.int("antall") }.asSingle,
            ) ?: 0
        }
    }
}
