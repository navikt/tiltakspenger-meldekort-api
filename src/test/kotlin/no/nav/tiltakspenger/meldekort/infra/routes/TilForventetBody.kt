package no.nav.tiltakspenger.meldekort.infra.routes

import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody

/**
 * Mapper request-hjelpernes `String?`-kontrakt til [ForventetBody].
 * `null` betyr at bodyen ikke assertes, tom streng betyr [ForventetBody.Tom], ellers [ForventetBody.Eksakt].
 */
fun String?.tilForventetBody(): ForventetBody? = when {
    this == null -> null
    this.isEmpty() -> ForventetBody.Tom
    else -> ForventetBody.Eksakt(this)
}
