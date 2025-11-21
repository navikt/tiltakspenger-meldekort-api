package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode.Companion.TIDSPUNKT_BRUKER_KAN_FYLLE_UT_MELDEPERIODE_FOR
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

data class Sak(
    val id: SakId,
    val saksnummer: String,
    val fnr: Fnr,
    val meldeperioder: List<Meldeperiode>,
    val arenaMeldekortStatus: ArenaMeldekortStatus,
    val harSoknadUnderBehandling: Boolean,
    val kanSendeInnHelgForMeldekort: Boolean,
) {

    fun erLik(otherSak: Sak): Boolean {
        // Enkelte felter er ikke relevante for å avgjøre om to saker er like, dermed kopierer vi disse feltene før sammenligningen
        return this.copy(
            arenaMeldekortStatus = otherSak.arenaMeldekortStatus,
        ) == otherSak
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

fun Periode.kanFyllesUtFraOgMed(): LocalDateTime =
    this.tilOgMed.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
        .atTime(TIDSPUNKT_BRUKER_KAN_FYLLE_UT_MELDEPERIODE_FOR).also {
            if (!this.inneholder(it.toLocalDate())) {
                throw IllegalArgumentException("$it er utenfor perioden $this")
            }
        }

fun SakTilMeldekortApiDTO.tilSak(): Sak {
    val sakId = SakId.fromString(this.sakId)
    val fnr = Fnr.fromString(this.fnr)

    val meldeperioder = this.meldeperioder.map {
        val periode = Periode(
            it.fraOgMed,
            it.tilOgMed,
        )
        Meldeperiode(
            id = MeldeperiodeId.fromString(it.id),
            kjedeId = MeldeperiodeKjedeId(it.kjedeId),
            versjon = it.versjon,
            sakId = sakId,
            saksnummer = this.saksnummer,
            fnr = fnr,
            periode = periode,
            opprettet = it.opprettet,
            maksAntallDagerForPeriode = it.antallDagerForPeriode,
            girRett = it.girRett,
            kanFyllesUtFraOgMed = periode.kanFyllesUtFraOgMed(),
        )
    }.sortedBy { it.periode.fraOgMed }

    return Sak(
        id = sakId,
        fnr = fnr,
        saksnummer = this.saksnummer,
        meldeperioder = meldeperioder,
        arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
        harSoknadUnderBehandling = harSoknadUnderBehandling,
        kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
    )
}
