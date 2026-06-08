package no.nav.tiltakspenger.meldekort.sending.infra

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.infra.brukersMeldekortFromRow
import no.nav.tiltakspenger.meldekort.sending.SendMeldekortRepo
import java.time.LocalDateTime

class SendMeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : SendMeldekortRepo {
    override fun hentMeldekortForSendingTilSaksbehandling(sessionContext: SessionContext?): List<BrukersMeldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """ 
                        select
                            *
                        from meldekort_bruker
                        where sendt_til_saksbehandling is null
                            and journalpost_id is not null
                            and mottatt is not null
                            order by mottatt desc
                    """,
                )
                    .map { row -> brukersMeldekortFromRow(row, session) }.asList,
            )
        }
    }

    override fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """ 
                        update meldekort_bruker set
                            sendt_til_saksbehandling = :sendtTidspunkt
                        where id = :id
                    """,
                    "id" to id.toString(),
                    "sendtTidspunkt" to sendtTidspunkt,
                ).asUpdate,
            )
        }
    }
}
