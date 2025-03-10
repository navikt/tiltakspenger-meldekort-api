package no.nav.tiltakspenger.meldekort.domene

import java.util.*

@JvmInline
value class VarselId(
    private val value: String,
) {
    override fun toString() = value

    companion object {
        fun random() = VarselId(UUID.randomUUID().toString())
    }
}
