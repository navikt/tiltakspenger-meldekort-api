package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.Innvilgelsesperioder

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

private data class PeriodeDb(
    val fraOgMed: String,
    val tilOgMed: String,
)

fun Innvilgelsesperioder.tilDb(): String = serialize(
    this.map {
        PeriodeDb(
            fraOgMed = it.fraOgMed.toString(),
            tilOgMed = it.tilOgMed.toString(),
        )
    },
)

fun String.tilInnvilgelsesperioder(): Innvilgelsesperioder = deserialize<Innvilgelsesperioder>(this)
