package no.nav.tiltakspenger.meldekort.api.tilgang

enum class Rolle {
    SAKSBEHANDLER,
    FORTROLIG_ADRESSE,
    STRENGT_FORTROLIG_ADRESSE,
    SKJERMING,
    LAGE_HENDELSER,
    DRIFT, // Systemadministrator (oss)
    BESLUTTER,
    ADMINISTRATOR, // Saksbehandlers administrator (superbruker)
}
