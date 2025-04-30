package no.nav.tiltakspenger.meldekort.auth

enum class TexasIdentityProvider(val identityProvider: String) {
    AZUREAD("azuread"),
    TOKENX("tokenx"),
    MASKINPORTEN("maskinporten"),
    IDPORTEN("idporten"),
}
