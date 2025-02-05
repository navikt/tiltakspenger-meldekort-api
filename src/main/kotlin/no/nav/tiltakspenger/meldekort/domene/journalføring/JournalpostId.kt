package no.nav.tiltakspenger.meldekort.domene.journalføring

@JvmInline
value class JournalpostId(
    private val value: String,
) {
    override fun toString() = value
}
