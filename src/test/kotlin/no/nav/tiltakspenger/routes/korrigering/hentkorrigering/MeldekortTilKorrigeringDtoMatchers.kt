package no.nav.tiltakspenger.routes.korrigering.hentkorrigering

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering.MeldekortTilKorrigeringDTO
import java.time.LocalDateTime

fun MeldekortTilKorrigeringDTO.shouldBe(
    forrigeMeldekort: MeldekortTilBrukerDTO,
    tilUtfylling: MeldekortTilKorrigeringDTO.PreutfyltKorrigeringDTO,
) {
    this shouldBe MeldekortTilKorrigeringDTO(
        forrigeMeldekort = forrigeMeldekort,
        tilUtfylling = tilUtfylling,
    )
}

fun forventetPreutfyltKorrigering(
    meldeperiodeId: String,
    kjedeId: String,
    dager: List<MeldekortDagTilBrukerDTO>,
    periode: PeriodeDTO,
    mottattTidspunktSisteMeldekort: LocalDateTime,
    maksAntallDagerForPeriode: Int = 10,
    kanSendeInnHelg: Boolean = false,
): MeldekortTilKorrigeringDTO.PreutfyltKorrigeringDTO {
    return MeldekortTilKorrigeringDTO.PreutfyltKorrigeringDTO(
        meldeperiodeId = meldeperiodeId,
        kjedeId = kjedeId,
        dager = dager,
        periode = periode,
        mottattTidspunktSisteMeldekort = mottattTidspunktSisteMeldekort,
        maksAntallDagerForPeriode = maksAntallDagerForPeriode,
        kanSendeInnHelg = kanSendeInnHelg,
    )
}
