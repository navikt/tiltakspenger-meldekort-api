package no.nav.tiltakspenger.meldekort.api.routes.dto

data class MeldekortBeregningDTO(
    val antallDeltattUtenLønn: Int,
    val antallDeltattMedLønn: Int,
    val antallIkkeDeltatt: Int,
    val antallSykDager: Int,
    val antallSykBarnDager: Int,
    val antallVelferdGodkjentAvNav: Int,
    val antallVelferdIkkeGodkjentAvNav: Int,
    val antallFullUtbetaling: Int,
    val antallDelvisUtbetaling: Int,
    val antallIngenUtbetaling: Int,
    val sumDelvis: Int,
    val sumFull: Int,
    val sumTotal: Int,
)
