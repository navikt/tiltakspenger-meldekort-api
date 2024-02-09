package no.nav.tiltakspenger.meldekort.api.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf

internal fun <T> String.hent(
    params: Map<String, Any> = emptyMap(),
    session: Session,
    rowMapping: (Row) -> T,
): T? {
    return session.run(queryOf(this, params).map { row -> rowMapping(row) }.asSingle)
}

internal fun <T> String.hentListe(
    params: Map<String, Any> = emptyMap(),
    session: Session,
    rowMapping: (Row) -> T,
): List<T> {
    return session.run(queryOf(this, params).map { row -> rowMapping(row) }.asList)
}

internal fun Row.booleanOrNull(name: String): Boolean? = this.anyOrNull(name)?.let { this.boolean(name) }

fun <T> withTransaction(block: (TransactionalSession) -> T): T {
    sessionOf(DataSource.hikariDataSource).use { session ->
        session.transaction { tx ->
            return block(tx)
        }
    }
}
