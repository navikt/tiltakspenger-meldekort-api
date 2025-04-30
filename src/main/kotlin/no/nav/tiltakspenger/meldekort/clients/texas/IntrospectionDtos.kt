package no.nav.tiltakspenger.meldekort.clients.texas

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tiltakspenger.meldekort.auth.TexasIdentityProvider

data class TexasIntrospectionRequest(
    @JsonProperty("identity_provider") val identityProvider: TexasIdentityProvider,
    val token: String,
)

data class TexasIntrospectionResponse(
    val active: Boolean,
    @JsonInclude(JsonInclude.Include.NON_NULL) val error: String?,
    @JsonAnySetter @get:JsonAnyGetter val other: Map<String, Any?> = mutableMapOf(),
)
