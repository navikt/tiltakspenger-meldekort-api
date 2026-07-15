package no.nav.tiltakspenger.meldekort.jobb

/**
 * Domenets utfall av én jobbkjøring, uavhengig av scheduler-biblioteket.
 * Mappes til `TaskResultat` fra libs i infra-laget (`Jobber.kt`), slik at domeneservicene ikke trenger å importere jobber-biblioteket.
 * TODO jah: Vi bør splitte opp TaskResultat ut i en egen domenemodul under jobber og fjerne denne.
 */
enum class JobbResultat {
    /** Jobben fant ikke noe arbeid. */
    IngenArbeid,

    /** Jobben fant og utførte (eller forsøkte å utføre) arbeid. */
    UtførteArbeid,

    /** Jobben feilet før den fikk gjort noe, og har selv logget feilen. */
    Feilet,
}

/**
 * Slår sammen resultatene fra flere del-jobber til ett [JobbResultat] for jobben som helhet.
 * [JobbResultat.Feilet] vinner over [JobbResultat.UtførteArbeid], som vinner over [JobbResultat.IngenArbeid].
 * En tom samling regnes som [JobbResultat.IngenArbeid].
 */
fun Iterable<JobbResultat>.tilSamletResultat(): JobbResultat = when {
    any { it == JobbResultat.Feilet } -> JobbResultat.Feilet
    any { it == JobbResultat.UtførteArbeid } -> JobbResultat.UtførteArbeid
    else -> JobbResultat.IngenArbeid
}
