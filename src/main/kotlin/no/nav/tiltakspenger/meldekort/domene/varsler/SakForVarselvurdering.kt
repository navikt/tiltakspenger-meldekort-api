package no.nav.tiltakspenger.meldekort.domene.varsler

import no.nav.tiltakspenger.meldekort.domene.Sak
import java.time.LocalDateTime

/**
 * En sak som står i kø for varselvurdering, sammen med tidspunktet den sist ble flagget for
 * vurdering.
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
    val sak: Sak,
    val sistFlaggetTidspunkt: LocalDateTime?,
)
