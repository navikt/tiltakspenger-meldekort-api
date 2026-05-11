package no.nav.tiltakspenger.meldekort.varsler

import java.util.UUID

@JvmInline
value class VarselId(
    private val value: String,
) {
    override fun toString() = value

    companion object {
        fun random() = VarselId(UUID.randomUUID().toString())
    }
}
