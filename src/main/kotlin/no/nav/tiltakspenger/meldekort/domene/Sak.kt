package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.SakDTO

data class Sak(
    val id: SakId,
    val saksnummer: String,
    val fnr: Fnr,
    val meldeperioder: List<Meldeperiode>,
    val arenaMeldekortStatus: ArenaMeldekortStatus,
) {

    fun erLik(otherSak: Sak): Boolean {
        return this.copy(arenaMeldekortStatus = otherSak.arenaMeldekortStatus) == otherSak
    }
}

enum class ArenaMeldekortStatus {
    UKJENT,
    HAR_MELDEKORT,
    HAR_IKKE_MELDEKORT,
}

fun SakDTO.tilSak(): Sak {
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
    }

    return Sak(
        id = SakId.fromString(this.sakId),
        fnr = Fnr.fromString(this.fnr),
        saksnummer = this.saksnummer,
        meldeperioder = meldeperioder,
        arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
    )
}
