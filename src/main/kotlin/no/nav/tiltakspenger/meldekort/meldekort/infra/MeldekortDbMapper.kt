package no.nav.tiltakspenger.meldekort.meldekort.infra

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.meldekort.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.MeldeperiodePostgresRepo

/**
 * Felles rad-mapper for `meldekort_bruker`. Deles av postgres-repoene i meldekort-, sending- og
 * journalføring-domenet slik at de slipper å duplisere mapping-logikken.
 */
internal fun brukersMeldekortFromRow(
    row: Row,
    session: Session,
): BrukersMeldekort {
    val id = MeldekortId.fromString(row.string("id"))
    val meldeperiodeId = MeldeperiodeId.fromString(row.string("meldeperiode_id"))

    val meldeperiode = MeldeperiodePostgresRepo.hentForId(meldeperiodeId, session)
    requireNotNull(meldeperiode) { "Fant ingen meldeperiode for $id" }

    return BrukersMeldekort(
        id = id,
        meldeperiode = meldeperiode,
        mottatt = row.localDateTimeOrNull("mottatt"),
        deaktivert = row.localDateTimeOrNull("deaktivert"),
        dager = row.string("dager").toMeldekortDager(),
        journalpostId = row.stringOrNull("journalpost_id")?.let { JournalpostId(it) },
        journalføringstidspunkt = row.localDateTimeOrNull("journalføringstidspunkt"),
        korrigering = row.boolean("korrigering"),
        locale = row.stringOrNull("locale"),
    )
}
