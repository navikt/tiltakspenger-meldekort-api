package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import java.time.LocalDateTime

data class PreutfyltKorrigering(
    val meldeperiodeId: MeldeperiodeId,
    val kjedeId: MeldeperiodeKjedeId,
    val dager: List<MeldekortDag>,
    val periode: Periode,
    val mottattTidspunktSisteMeldekort: LocalDateTime,
)
