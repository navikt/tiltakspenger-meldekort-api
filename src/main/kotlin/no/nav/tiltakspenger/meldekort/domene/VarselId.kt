package no.nav.tiltakspenger.meldekort.domene

@JvmInline
value class VarselId(
    private val value: String,
) {
    override fun toString() = value
}
