package no.nav.tiltakspenger.meldekort.journalføring.infra

import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.meldekort.journalføring.JournalføringRepo
import no.nav.tiltakspenger.meldekort.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.infra.brukersMeldekortFromRow
import java.time.LocalDateTime

class JournalføringPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : JournalføringRepo {
    override fun hentDeSomSkalJournalføres(limit: Int, sessionContext: SessionContext?): List<BrukersMeldekort> =
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                queryOf(
                    //language=sql
                    """
                    select mk.*
                    from meldekort_bruker mk
                    where mk.journalpost_id is null
                      and mk.mottatt is not null
                    limit :limit
                    """.trimIndent(),
                    mapOf("limit" to limit),
                ).map { row ->
                    brukersMeldekortFromRow(row, session)
                }.asList,
            )
        }

    override fun markerJournalført(
        meldekortId: MeldekortId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                queryOf(
                    //language=sql
                    """
                      update meldekort_bruker
                      set journalpost_id = :journalpost_id,
                          journalføringstidspunkt = :tidspunkt
                      where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to meldekortId.toString(),
                        "journalpost_id" to journalpostId.toString(),
                        "tidspunkt" to tidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }
}
