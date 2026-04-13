package no.nav.tiltakspenger.routes.korrigering.kankorrigeres

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering.KanKorrigereMeldekortDto

fun KanKorrigereMeldekortDto.shouldBe(
    kanKorrigeres: Boolean,
) {
    this shouldBe KanKorrigereMeldekortDto(
        kanKorrigeres = kanKorrigeres,
    )
}
