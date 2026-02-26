package no.nav.tiltakspenger.meldekort.db

fun prefixColumn(alias: String?): (label: String) -> String {
    val prefix = alias?.let { "$alias." } ?: ""

    return { label: String -> prefix + label }
}
