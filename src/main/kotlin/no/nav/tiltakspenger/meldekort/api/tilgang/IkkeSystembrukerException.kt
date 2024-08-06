package no.nav.tiltakspenger.meldekort.api.tilgang

class IkkeSystembrukerException(override val message: String = "Ikke systembruker") : RuntimeException(message)
