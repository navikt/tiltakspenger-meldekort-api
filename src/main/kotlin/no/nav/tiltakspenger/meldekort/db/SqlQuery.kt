package no.nav.tiltakspenger.meldekort.db

import kotliquery.Query
import org.intellij.lang.annotations.Language

fun sqlQuery(
    @Language("PostgreSQL")
    query: String,
    vararg params: Pair<String, Any?>,
): Query {
    return Query(query.trimIndent(), paramMap = mapOf(*params))
}
