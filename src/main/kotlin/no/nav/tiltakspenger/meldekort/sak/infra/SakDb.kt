package no.nav.tiltakspenger.meldekort.sak.infra

import no.nav.tiltakspenger.meldekort.sak.ArenaMeldekortStatus

private enum class ArenaMeldekortStatusDb {
    UKJENT,
    HAR_MELDEKORT,
    HAR_IKKE_MELDEKORT,
}

fun ArenaMeldekortStatus.tilDb(): String = when (this) {
    ArenaMeldekortStatus.UKJENT -> ArenaMeldekortStatusDb.UKJENT
    ArenaMeldekortStatus.HAR_MELDEKORT -> ArenaMeldekortStatusDb.HAR_MELDEKORT
    ArenaMeldekortStatus.HAR_IKKE_MELDEKORT -> ArenaMeldekortStatusDb.HAR_IKKE_MELDEKORT
}.toString()

fun String.tilArenaMeldekortStatus(): ArenaMeldekortStatus = when (ArenaMeldekortStatusDb.valueOf(this)) {
    ArenaMeldekortStatusDb.UKJENT -> ArenaMeldekortStatus.UKJENT
    ArenaMeldekortStatusDb.HAR_MELDEKORT -> ArenaMeldekortStatus.HAR_MELDEKORT
    ArenaMeldekortStatusDb.HAR_IKKE_MELDEKORT -> ArenaMeldekortStatus.HAR_IKKE_MELDEKORT
}
