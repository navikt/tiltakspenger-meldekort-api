@file:Suppress("unused", "ktlint")

package db.migration

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.meldekort.repository.tilMeldekortDagDbJson
import no.nav.tiltakspenger.meldekort.repository.toMeldekortDager
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

internal class V20__ikke_rett_status : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val logger = KotlinLogging.logger {}
        val dataSource = context.configuration.dataSource
        val sessionFactory = PostgresSessionFactory(dataSource, SessionCounter(logger))

        val alleMeldekortBruker = sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        select * from meldekort_bruker
                    """.trimIndent()
                ).map { row -> fromRow(row, session) }.asList
            )
        }

        alleMeldekortBruker.forEach { meldekort ->
            val meldeperiode = meldekort.meldeperiode

            //denne saken trenger ikke migrering
            if(meldekort.sakId.toString().contains("01JCN9")){
                return@forEach
            }

            val dager = meldeperiode.girRett.toList().zip(meldekort.dager).map { (girRett, meldekortDag) ->
                val harRettPåDenneDagen = girRett.second

                if (harRettPåDenneDagen) {
                    //hvis bruker har rett, så skal dagen være utfylt med noe fra før (??)
                    MeldekortDag(
                        dag = meldekortDag.dag,
                        status = meldekortDag.status,
                    )
                } else {
                    //har ikke rett - skal sette status til IKKE_RETT_TIL_TILTAKSPENGER (denne skal være IKKE_BESVART)
                    MeldekortDag(
                        dag = meldekortDag.dag,
                        status = MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                    )
                }
            }

            sessionFactory.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                            update meldekort_bruker
                            set dager = to_jsonb(:dager::jsonb)
                            where id = :id
                        """.trimIndent(),
                        "dager" to dager.tilMeldekortDagDbJson(),
                        "id" to meldekort.id.toString(),
                    ).asUpdate
                )
            }
        }
    }
}

private fun fromRow(
    row: Row,
    session: Session,
): Meldekort {
    val id = MeldekortId.fromString(row.string("id"))
    val meldeperiodeId = row.string("meldeperiode_id")

    val meldeperiode = MeldeperiodePostgresRepo.hentForId(MeldeperiodeId.fromString(meldeperiodeId), session)

    requireNotNull(meldeperiode) { "Fant ingen meldeperiode for $id" }

    return Meldekort(
        id = id,
        meldeperiode = meldeperiode,
        mottatt = row.localDateTimeOrNull("mottatt"),
        deaktivert = row.localDateTimeOrNull("deaktivert"),
        dager = row.string("dager").toMeldekortDager(),
        journalpostId = row.stringOrNull("journalpost_id")?.let { JournalpostId(it) },
        journalføringstidspunkt = row.localDateTimeOrNull("journalføringstidspunkt"),
        varselId = row.stringOrNull("varsel_id")?.let { VarselId(it) },
        erVarselInaktivert = row.boolean("varsel_inaktivert"),
    )
}