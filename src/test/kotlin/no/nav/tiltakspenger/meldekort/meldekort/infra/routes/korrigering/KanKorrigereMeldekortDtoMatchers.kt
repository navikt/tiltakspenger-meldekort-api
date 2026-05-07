package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.KanKorrigereMeldekortDto

fun KanKorrigereMeldekortDto.shouldBe(
    kanKorrigeres: Boolean,
) {
    this shouldBe KanKorrigereMeldekortDto(
        kanKorrigeres = kanKorrigeres,
    )
}
