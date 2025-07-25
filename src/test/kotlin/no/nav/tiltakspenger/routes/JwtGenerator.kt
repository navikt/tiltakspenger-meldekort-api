package no.nav.tiltakspenger.routes

import com.auth0.jwk.Jwk
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64
import java.util.UUID

data class JwtGenerator(
    private val jwkKeyId: String = UUID.randomUUID().toString(),
) {
    val keyPair: KeyPair = generateRsaKeyPair()
    val publicKey = keyPair.public as RSAPublicKey
    val privateKey = keyPair.private as RSAPrivateKey
    val jwkAsString = createJwk(jwkKeyId, publicKey)
    val jwk: Jwk by lazy {
        objectMapper.readTree(jwkAsString).get("keys").first().let {
            Jwk.fromValues(
                mapOf(
                    "alg" to "RS256",
                    "kty" to "RSA",
                    "use" to "sig",
                    "kid" to it.get("kid").asText(),
                    "n" to it.get("n").asText(),
                    "e" to it.get("e").asText(),
                ),
            )
        }
    }

    /**
     * @param saksbehandler Overstyrer preferredUsername, navIdent og groups. Defaultverdier er samme som ObjectMother.saksbehandler()
     */
    fun createJwtForSaksbehandler(
        jwtKeyId: String = jwkKeyId,
        issuer: String = "https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0",
        subject: String = "test-subject",
        preferredUsername: String? = "Sak.Behandler@nav.no",
        azpName: String? = "dev-fss:tpts:tiltakspenger-saksbehandling-api",
        azp: String? = "744e4092-4215-4e02-87df-a61aaf1b95b5",
        fnr: String? = FAKE_FNR,
        name: String = "Sak Behandler",
        audience: String = "c7adbfbb-1b1e-41f6-9b7a-af9627c04998",
        groups: List<String>? = listOf("ROLE_SAKSBEHANDLER"),
        saksbehandler: Saksbehandler? = null,
        expiresAt: Instant = Instant.now().plusSeconds(1800),
        issuedAt: Instant = Instant.now().minusSeconds(5),
        notBefore: Instant = Instant.now().minusSeconds(5),
    ): String {
        val algorithm = Algorithm.RSA256(null, privateKey)
        return JWT.create()
            .withKeyId(jwtKeyId)
            .withIssuer(issuer)
            .withSubject(subject)
            .withAudience(audience)
            .withExpiresAt(expiresAt)
            .withIssuedAt(issuedAt)
            .withNotBefore(notBefore)
            .withClaim("preferred_username", saksbehandler?.epost ?: preferredUsername)
            .withClaim("fnr", fnr)
            .withClaim("name", name)
            .withClaim("azp_name", azpName)
            .withClaim("azp", azp)
            .withClaim("groups", saksbehandler?.roller?.map { "ROLE_${it.name}" } ?: groups)
            .withClaim("ver", "2.0")
            .sign(algorithm)
    }

    fun createJwtForSystembruker(
        jwtKeyId: String = jwkKeyId,
        issuer: String = "https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0",
        subject: String = "test-subject",
        azpName: String? = "dev-fss:tpts:tiltakspenger-saksbehandling-api",
        azp: String? = "744e4092-4215-4e02-87df-a61aaf1b95b5",
        name: String = "Test Testesen",
        audience: String = "c7adbfbb-1b1e-41f6-9b7a-af9627c04998",
        roles: List<String>? = listOf("TEST_ROLLE_1", "TEST_ROLLE_2"),
        oid: String? = subject,
        expiresAt: Instant = Instant.now().plusSeconds(1800),
        issuedAt: Instant = Instant.now().minusSeconds(5),
        notBefore: Instant = Instant.now().minusSeconds(5),
    ): String {
        val algorithm = Algorithm.RSA256(null, privateKey)
        return JWT.create()
            .withKeyId(jwtKeyId)
            .withIssuer(issuer)
            .withSubject(subject)
            .withAudience(audience)
            .withExpiresAt(expiresAt)
            .withIssuedAt(issuedAt)
            .withNotBefore(notBefore)
            .withClaim("name", name)
            .withClaim("roles", roles)
            .withClaim("azp_name", azpName)
            .withClaim("azp", azp)
            .withClaim("oid", oid)
            .withClaim("idtyp", "app")
            .withClaim("ver", "2.0")
            .sign(algorithm)
    }

    fun createJwtForUser(
        jwtKeyId: String = jwkKeyId,
        issuer: String = "https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0",
        subject: String = "test-subject",
        azpName: String? = "dev-gcp:tpts:tiltakspenger-meldekort",
        azp: String? = "744e4092-4215-4e02-87df-a61aaf1b95b5",
        fnr: String? = FAKE_FNR,
        audience: String = "c7adbfbb-1b1e-41f6-9b7a-af9627c04998",
        expiresAt: Instant = Instant.now().plusSeconds(1800),
        issuedAt: Instant = Instant.now().minusSeconds(5),
        notBefore: Instant = Instant.now().minusSeconds(5),
    ): String {
        val algorithm = Algorithm.RSA256(null, privateKey)
        return JWT.create()
            .withKeyId(jwtKeyId)
            .withIssuer(issuer)
            .withSubject(subject)
            .withAudience(audience)
            .withExpiresAt(expiresAt)
            .withIssuedAt(issuedAt)
            .withNotBefore(notBefore)
            .withClaim("pid", fnr)
            .withClaim("azp_name", azpName)
            .withClaim("azp", azp)
            .withClaim("ver", "2.0")
            .sign(algorithm)
    }
}

private fun generateRsaKeyPair(): KeyPair {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    return keyPairGenerator.generateKeyPair()
}

fun createJwk(
    keyId: String,
    publicKey: RSAPublicKey,
): String {
    val modulus = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.modulus.toByteArray())
    val exponent = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.publicExponent.toByteArray())

    return """
            {"keys":[
        {
            "alg": "RS256",
            "kty": "RSA",
            "use": "sig",
            "kid": "$keyId",
            "n": "$modulus",
            "e": "$exponent"
        }]}
    """.trimIndent()
}
