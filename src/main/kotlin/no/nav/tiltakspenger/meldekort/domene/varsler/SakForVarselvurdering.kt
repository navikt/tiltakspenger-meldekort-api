package no.nav.tiltakspenger.meldekort.domene.varsler

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import java.time.LocalDateTime

/**
 * En sak som står i kø for varselvurdering. Inneholder kun de feltene [VurderVarselService]
 * faktisk trenger, ikke hele [no.nav.tiltakspenger.meldekort.domene.Sak]-aggregatet.
 *
 * Vi henter ikke meldeperioder her: jobben kjører hvert 10. sek og en N+1-spørring per sak ville
 * vært unødvendig dyrt. Per-sak-arbeidet (kjeder som mangler innsending) gjøres i
 * [no.nav.tiltakspenger.meldekort.repository.VarselMeldekortRepo.hentKjederSomManglerInnsending].
 *
 * [sistFlaggetTidspunkt] brukes som optimistisk lås: når jobben fullfører og kaller
 * [no.nav.tiltakspenger.meldekort.repository.SakVarselRepo.markerVarselVurdert], oppdateres
 * flagget KUN dersom [sistFlaggetTidspunkt] er uendret. Hvis det har kommet en ny hendelse
 * (f.eks. meldekort-innsending) som har kalt
 * [no.nav.tiltakspenger.meldekort.repository.SakVarselRepo.flaggForVarselvurdering] underveis,
 * vil tidspunktet ha endret seg og markeringen feiler – saken blir da plukket opp i neste
 * jobbkjøring og vurdert på nytt.
 */
data class SakForVarselvurdering(
    val sakId: SakId,
    val saksnummer: String,
    val fnr: Fnr,
    val sistFlaggetTidspunkt: LocalDateTime?,
)
