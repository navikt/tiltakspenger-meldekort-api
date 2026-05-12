package no.nav.tiltakspenger.meldekort.meldekortvedtak.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldekortvedtakRepo

class MeldekortvedtakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldekortvedtakRepo {

    private val logger = KotlinLogging.logger {}

    override fun lagre(meldekortvedtak: Meldekortvedtak, sessionContext: SessionContext?) {
        sessionFactory.withSession(sessionContext) { session ->
            // Meldekortvedtak er immutable etter iverksettelse og dedupliseres på id, jf. V37__meldekortvedtak.sql.
            // ON CONFLICT DO NOTHING gjør lagring idempotent og trygt ved samtidighet mellom PODs / retries fra innsender.
            val antallRader = session.run(
                sqlQuery(
                    """
                    INSERT INTO meldekortvedtak (
                        id,
                        sak_id,
                        opprettet,
                        er_korrigering,
                        er_automatisk_behandlet,
                        meldeperiodebehandlinger
                    ) VALUES (
                        :id,
                        :sak_id,
                        :opprettet,
                        :er_korrigering,
                        :er_automatisk_behandlet,
                        to_jsonb(:meldeperiodebehandlinger::jsonb)
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    "id" to meldekortvedtak.id.toString(),
                    "sak_id" to meldekortvedtak.sakId.toString(),
                    "opprettet" to meldekortvedtak.opprettet,
                    "er_korrigering" to meldekortvedtak.erKorrigering,
                    "er_automatisk_behandlet" to meldekortvedtak.erAutomatiskBehandlet,
                    "meldeperiodebehandlinger" to meldekortvedtak.meldeperiodebehandlinger.tilMeldeperiodebehandlingerDbJson(),
                ).asUpdate,
            )
            if (antallRader == 0) {
                logger.info { "Hoppet over lagring av meldekortvedtak ${meldekortvedtak.id} - finnes allerede (sak ${meldekortvedtak.sakId})" }
            } else {
                logger.info { "Lagret meldekortvedtak ${meldekortvedtak.id} for sak ${meldekortvedtak.sakId}" }
            }
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
