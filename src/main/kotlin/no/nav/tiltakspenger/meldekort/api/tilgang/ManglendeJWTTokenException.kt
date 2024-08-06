package no.nav.tiltakspenger.meldekort.api.tilgang

class ManglendeJWTTokenException(override val message: String = "JWTToken ikke funnet") : RuntimeException(message)
