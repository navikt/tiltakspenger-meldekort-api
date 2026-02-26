package no.nav.tiltakspenger.meldekort.db

import kotlin.let

fun prefixColumn(alias: String?): (label: String) -> String {
    val prefix = alias?.let { "$alias." } ?: ""

    return { label: String -> prefix + label }
}
