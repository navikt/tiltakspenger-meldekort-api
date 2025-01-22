package no.nav.tiltakspenger.meldekort.domene

/**
 * TODO jah: Del denne opp i BrukersMeldekortStatus og MeldeperiodeStatus
 */
enum class MeldekortStatus {
    INNSENDT,

    /** TODO jah: Bør endres til RETT_TIL_TILTAKSPENGER */
    KAN_UTFYLLES,

    /** TODO jah: Bør endres til IKKE_RETT_TIL_TILTAKSPENGER */
    KAN_IKKE_UTFYLLES,
}
