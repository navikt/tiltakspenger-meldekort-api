package no.nav.tiltakspenger.meldekort.api.tilgang

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import mu.KotlinLogging
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.tiltakspenger.meldekort.api.AdRolle
import no.nav.tiltakspenger.meldekort.api.Configuration
import java.util.*

private val LOG = KotlinLogging.logger {}

class InnloggetBrukerProvider(
    private val allAvailableRoles: List<AdRolle> = Configuration.alleAdRoller(),
) {
    private fun finnRolleMedUUID(uuidFraClaim: UUID) =
        allAvailableRoles.single { configRole -> configRole.objectId == uuidFraClaim }

    private fun List<UUID>.mapFromUUIDToRoleName(): List<Rolle> =
        this.map { LOG.info { "Mapper rolle $it" }; it }
            .map { finnRolleMedUUID(it).name }

    private fun hentSaksbehandler(principal: TokenValidationContextPrincipal): Saksbehandler {
        val ident = requireNotNull(principal.getClaim("azure", "NAVident")) { "NAVident er null i token" }
        val roller = principal.getListClaim("azure", "groups").map { UUID.fromString(it) }.mapFromUUIDToRoleName()
        return Saksbehandler(
            navIdent = ident,
            roller = roller,
        )
    }

    fun krevInnloggetSaksbehandler(call: ApplicationCall): Saksbehandler {
        val principal = call.authentication.principal<TokenValidationContextPrincipal>() ?: throw ManglendeJWTTokenException()
        return hentSaksbehandler(principal)
    }

    fun krevSystembruker(call: ApplicationCall) {
        val principal = call.authentication.principal<TokenValidationContextPrincipal>() ?: throw ManglendeJWTTokenException()
        if (!principal.getClaim("azure", "idtyp").equals("app")) throw IkkeSystembrukerException()
    }
}

internal fun TokenValidationContextPrincipal.getClaim(issuer: String, name: String): String? =
    this.context
        .getClaims(issuer)
        ?.getStringClaim(name)

internal fun TokenValidationContextPrincipal.getListClaim(issuer: String, name: String): List<String> =
    this.context
        .getClaims(issuer)
        ?.getAsList(name) ?: emptyList()
