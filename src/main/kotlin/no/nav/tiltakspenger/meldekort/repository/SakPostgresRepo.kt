package no.nav.tiltakspenger.meldekort.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.MicrofrontendStatus
import no.nav.tiltakspenger.meldekort.domene.Sak
import java.time.Clock

class SakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : SakRepo {

    override fun lagre(
        sak: Sak,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    insert into sak (
                        id,
                        saksnummer,
                        fnr,
                        arena_meldekort_status,
                        har_soknad_under_behandling
                    ) values (
                        :id,
                        :saksnummer,
                        :fnr,
                        :arena_meldekort_status,
                        :har_soknad_under_behandling
                    )
                    """,
                    "id" to sak.id.toString(),
                    "saksnummer" to sak.saksnummer,
                    "fnr" to sak.fnr.verdi,
                    "arena_meldekort_status" to sak.arenaMeldekortStatus.tilDb(),
                    "har_soknad_under_behandling" to sak.harSoknadUnderBehandling,
                ).asUpdate,
            )
        }
    }

    /** Oppdaterer fnr og søknadsbehandlingstatus på en sak */
    override fun oppdater(
        sak: Sak,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    update sak set 
                        fnr = :fnr,
                        har_soknad_under_behandling = :har_soknad_under_behandling
                    where id = :id
                    """,
                    "id" to sak.id.toString(),
                    "fnr" to sak.fnr.verdi,
                    "har_soknad_under_behandling" to sak.harSoknadUnderBehandling,
                ).asUpdate,
            )
        }
    }

    override fun oppdaterStatusForMicrofrontend(
        sakId: SakId,
        aktiv: Boolean,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    update sak set 
                        microfrontend_status = :microfrontend_status
                    where id = :id
                    """,
                    "id" to sakId.toString(),
                    "microfrontend_status" to if (aktiv) MicrofrontendStatus.AKTIV.toString() else MicrofrontendStatus.INAKTIV.toString(),
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

    override fun hent(
        id: SakId,
        sessionContext: SessionContext?,
    ): Sak? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select * from sak where id = :id",
                    "id" to id.toString(),
                ).map { row -> fromRow(row, true, session) }.asSingle,
            )
        }
    }

    override fun hentTilBruker(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Sak? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select * from sak where fnr = :fnr",
                    "fnr" to fnr.verdi,
                ).map { row -> fromRow(row, false, session) }.asSingle,
            )
        }
    }

    override fun hentSakerUtenArenaStatus(sessionContext: SessionContext?): List<Sak> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    "select * from sak where arena_meldekort_status = :ukjent_status",
                    "ukjent_status" to ArenaMeldekortStatus.UKJENT.tilDb(),
                ).map { row -> fromRow(row, false, session) }.asList,
            )
        }
    }

    override fun hentSakerHvorMicrofrontendSkalAktiveres(sessionContext: SessionContext?, clock: Clock): List<Sak> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        SELECT s.*
                        FROM sak s
                        WHERE s.microfrontend_status <> :aktivStatus
                          AND EXISTS (
                              SELECT 1
                              FROM meldeperiode m
                              WHERE m.sak_id = s.id
                                AND (
                                    EXISTS (
                                        SELECT 1
                                        FROM jsonb_each_text(m.gir_rett) kv(key, value)
                                        WHERE value::boolean
                                    )
                                    AND (
                                        m.opprettet > :offset
                                        OR m.til_og_med > :offset
                                    )
                                )
                          );
                    """.trimIndent(),
                    "offset" to nå(clock).minusMonths(1),
                    "aktivStatus" to MicrofrontendStatus.AKTIV.toString(),
                ).map { row -> fromRow(row, false, session) }.asList,
            )
        }
    }

    override fun hentSakerHvorMicrofrontendSkalInaktiveres(sessionContext: SessionContext?, clock: Clock): List<Sak> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        SELECT s.*
                        FROM sak s
                        WHERE s.microfrontend_status <> :inaktivStatus
                          AND NOT EXISTS (
                            SELECT 1
                            FROM meldeperiode m
                            WHERE m.sak_id = s.id
                              AND (
                                EXISTS (
                                    SELECT 1
                                    FROM jsonb_each_text(m.gir_rett) kv(key, value)
                                    WHERE value::boolean
                                )
                                AND (
                                    m.opprettet > :offset
                                    OR m.til_og_med > :offset
                                )
                              )
                          );
                    """.trimIndent(),
                    "offset" to nå(clock).minusMonths(1),
                    "inaktivStatus" to MicrofrontendStatus.INAKTIV.toString(),
                ).map { row -> fromRow(row, false, session) }.asList,
            )
        }
    }

    companion object {
        private fun fromRow(row: Row, medMeldeperioder: Boolean, session: Session): Sak {
            val sakId = SakId.fromString(row.string("id"))

            val meldeperioder =
                if (medMeldeperioder) MeldeperiodePostgresRepo.hentForSakId(sakId, session) else emptyList()

            return Sak(
                id = sakId,
                saksnummer = row.string("saksnummer"),
                fnr = Fnr.fromString(row.string("fnr")),
                meldeperioder = meldeperioder,
                arenaMeldekortStatus = row.string("arena_meldekort_status").tilArenaMeldekortStatus(),
                harSoknadUnderBehandling = row.boolean("har_soknad_under_behandling"),
            )
        }
    }
}
