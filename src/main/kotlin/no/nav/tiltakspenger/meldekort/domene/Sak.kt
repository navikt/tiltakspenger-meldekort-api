package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.periodisering.Periode

data class Sak(
    val id: SakId,
    val saksnummer: String,
    val fnr: Fnr,
    val meldeperioder: List<Meldeperiode>,
    val arenaMeldekortStatus: ArenaMeldekortStatus,
    val harSoknadUnderBehandling: Boolean,
) {

    fun erLik(otherSak: Sak): Boolean {
        return this.id == otherSak.id && this.saksnummer == otherSak.saksnummer &&
            this.fnr == otherSak.fnr && this.meldeperioder == otherSak.meldeperioder &&
            this.arenaMeldekortStatus == otherSak.arenaMeldekortStatus &&
            this.harSoknadUnderBehandling == otherSak.harSoknadUnderBehandling
    }

    init {
        meldeperioder.zipWithNext().forEach { (a, b) ->
            require(a.periode.tilOgMed < b.periode.fraOgMed || (a.periode == b.periode && a.versjon < b.versjon)) {
                "Meldeperioder må være sortert etter periode og versjon. Fikk $a før $b"
            }
        }
    }
}

enum class ArenaMeldekortStatus {
    UKJENT,
    HAR_MELDEKORT,
    HAR_IKKE_MELDEKORT,
}

fun SakTilMeldekortApiDTO.tilSak(): Sak {
    val sakId = SakId.fromString(this.sakId)
    val fnr = Fnr.fromString(this.fnr)

    val meldeperioder = this.meldeperioder.map {
        Meldeperiode(
            id = MeldeperiodeId.fromString(it.id),
            kjedeId = MeldeperiodeKjedeId(it.kjedeId),
            versjon = it.versjon,
            sakId = sakId,
            saksnummer = this.saksnummer,
            fnr = fnr,
            periode = Periode(
                it.fraOgMed,
                it.tilOgMed,
            ),
            opprettet = it.opprettet,
            maksAntallDagerForPeriode = it.antallDagerForPeriode,
            girRett = it.girRett,
        )
    }.sortedBy { it.periode.fraOgMed }

    return Sak(
        id = sakId,
        fnr = fnr,
        saksnummer = this.saksnummer,
        meldeperioder = meldeperioder,
        arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
        harSoknadUnderBehandling = harSoknadUnderBehandling,
    )
}
