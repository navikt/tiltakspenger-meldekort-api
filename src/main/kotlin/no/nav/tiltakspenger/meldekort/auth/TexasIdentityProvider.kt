package no.nav.tiltakspenger.meldekort.auth

enum class TexasIdentityProvider(val value: String) {
    AZUREAD("azuread"),
    TOKENX("tokenx"),
    MASKINPORTEN("maskinporten"),
    IDPORTEN("idporten"),
}
