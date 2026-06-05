package no.nav.tiltakspenger.meldekort.meldekortvedtak.infra

import kotliquery.Session
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldekortvedtakRepo

/**
 * Lesesiden for meldekortvedtak (CQRS).
 *
 * Skrivesiden (INSERT) bor i [no.nav.tiltakspenger.meldekort.mottak.infra.MottakPostgresRepo.lagreMeldekortvedtak].
 * Endres skjemaet for `meldekortvedtak`-tabellen må begge stedene oppdateres.
 */
class MeldekortvedtakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldekortvedtakRepo {
    override fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): List<Meldekortvedtak> {
        return sessionFactory.withSession(sessionContext) { session ->
            hentForSakId(sakId, session)
        }
    }

    companion object {
        fun hentForSakId(sakId: SakId, session: Session): List<Meldekortvedtak> =
            session.run(
                sqlQuery(
                    """
                    SELECT
                        id,
                        opprettet,
                        er_korrigering,
                        er_automatisk_behandlet,
                        meldeperiodebehandlinger
                    FROM meldekortvedtak
                    WHERE sak_id = :sak_id
                    ORDER BY opprettet
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row ->
                    Meldekortvedtak(
                        id = VedtakId.fromString(row.string("id")),
                        sakId = sakId,
                        opprettet = row.localDateTime("opprettet"),
                        erKorrigering = row.boolean("er_korrigering"),
                        erAutomatiskBehandlet = row.boolean("er_automatisk_behandlet"),
                        meldeperiodebehandlinger = row.string("meldeperiodebehandlinger").tilMeldeperiodebehandlinger(),
                    )
                }.asList,
            )
    }
}
