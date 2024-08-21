package no.nav.tiltakspenger.meldekort.api.exceptions

class UgyldigRequestException(
    override val message: String = "Ugyldig request",
) : RuntimeException(message)

class IkkeFunnetException(
    override val message: String,
) : RuntimeException(message)

class IkkeImplementertException(
    override val message: String,
) : RuntimeException(message)
