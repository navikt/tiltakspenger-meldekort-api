package no.nav.tiltakspenger.meldekort.api.tilgang

data class Saksbehandler(
    val navIdent: String,
    val roller: List<Rolle>,
) {
    fun isAdmin() = roller.contains(Rolle.ADMINISTRATOR)
    fun isSaksbehandler() = roller.contains(Rolle.SAKSBEHANDLER)
    fun isBeslutter() = roller.contains(Rolle.BESLUTTER)
}
