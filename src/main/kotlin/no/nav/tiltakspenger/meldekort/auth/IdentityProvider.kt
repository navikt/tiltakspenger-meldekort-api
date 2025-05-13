package no.nav.tiltakspenger.meldekort.auth

enum class IdentityProvider(val value: String) {
    AZUREAD("azuread"),
    TOKENX("tokenx"),
    MASKINPORTEN("maskinporten"),
    IDPORTEN("idporten"),
}
