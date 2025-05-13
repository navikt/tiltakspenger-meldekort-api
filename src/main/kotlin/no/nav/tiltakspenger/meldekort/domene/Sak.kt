package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.service.SakDTO

data class Sak(
    val id: SakId,
    val saksnummer: String,
    val fnr: Fnr,
    val meldeperioder: MeldeperioderForSak,
    val arenaMeldekortStatus: ArenaMeldekortStatus,
)

/** Dette er alle periodene som det vil genereres meldekort fra for hele innvilgelsesperioden på denne saken
 *  Kun fraOgMed/tilOgMed datoene, inkluderer ikke hvilke dager som gir rett osv.
 *  (Hvis vi genererer alle meldeperiodene fram i tid i sbh-api, så trenger vi ikke denne lengre :D)
 * */
data class MeldeperioderForSak(
    val verdi: List<Periode>,
) : List<Periode> by verdi

enum class ArenaMeldekortStatus {
    UKJENT,
    HAR_MELDEKORT,
    HAR_IKKE_MELDEKORT,
}

fun SakDTO.tilSak(): Sak {
    return Sak(
        id = SakId.fromString(this.sakId),
        saksnummer = this.saksnummer,
        fnr = Fnr.fromString(this.fnr),
        meldeperioder = MeldeperioderForSak(this.meldeperioder.map { it.toDomain() }),
        arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
    )
}
