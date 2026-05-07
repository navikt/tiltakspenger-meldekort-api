package no.nav.tiltakspenger.meldekort.infra.db

fun prefixColumn(alias: String?): (label: String) -> String {
    val prefix = alias?.let { "$alias." } ?: ""

    return { label: String -> prefix + label }
}
