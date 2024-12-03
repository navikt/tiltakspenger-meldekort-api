package no.nav.tiltakspenger.meldekort.db

import kotliquery.Query
import org.intellij.lang.annotations.Language

fun sqlQuery(
    @Language("PostgreSQL")
    query: String,
    paramMap: Map<String, Any?>? = null,
): Query {
    return Query(query.trimIndent(), paramMap = paramMap ?: emptyMap())
}
