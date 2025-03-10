package no.nav.tiltakspenger.meldekort.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.domene.senesteTilOgMedDatoForInnsending
import java.time.LocalDateTime

val logger = KotlinLogging.logger {}

class MeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MeldekortRepo {

    override fun lagre(meldekort: Meldekort, sessionContext: SessionContext?) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    insert into meldekort_bruker (
                        id,
                        meldeperiode_id,
                        sak_id,
                        mottatt,
                        dager,
                        journalpost_id,
                        journalføringstidspunkt,
                        varsel_id,
                        varsel_inaktivert
                    ) values (
                        :id,
                        :meldeperiode_id,
                        :sak_id,
                        :mottatt,
                        to_jsonb(:dager::jsonb),
                        :journalpost_id,
                        :tidspunkt,
                        :varsel_id,
                        :erVarselInaktivert
                    )
                """,
                    "id" to meldekort.id.toString(),
                    "meldeperiode_id" to meldekort.meldeperiode.id.toString(),
                    "sak_id" to meldekort.sakId.toString(),
                    "mottatt" to meldekort.mottatt,
                    "dager" to meldekort.dager.toDbJson(),
                    "journalpost_id" to meldekort.journalpostId?.toString(),
                    "tidspunkt" to meldekort.journalføringstidspunkt,
                    "varsel_id" to meldekort.varselId?.toString(),
                    "erVarselInaktivert" to meldekort.erVarselInaktivert,
                ).asUpdate,
            )
        }
    }

    override fun lagreFraBruker(lagreKommando: LagreMeldekortFraBrukerKommando, sessionContext: SessionContext?) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    update meldekort_bruker set
                        mottatt = :mottatt,
                        dager = to_jsonb(:dager::jsonb)
                    where id = :id
                    """,
                    "id" to lagreKommando.id.toString(),
                    "mottatt" to lagreKommando.mottatt,
                    "dager" to lagreKommando.dager.toDbJson(),
                ).asUpdate,
            )
        }
    }

    override fun oppdater(meldekort: Meldekort, sessionContext: SessionContext?) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    update meldekort_bruker set 
                        varsel_id = :varsel_id,
                        varsel_inaktivert = :varsel_inaktivert
                    where id = :id
                """,
                    "id" to meldekort.id.toString(),
                    "varsel_id" to meldekort.varselId?.toString(),
                    "varsel_inaktivert" to meldekort.erVarselInaktivert,
                ).asUpdate,
            )
        }
    }

    /** TODO jah: Denne må returnere en liste dersom vi støtter flere innsender på samme meldeperiode */
    override fun hentMeldekortForMeldeperiodeId(
        meldeperiodeId: MeldeperiodeId,
        sessionContext: SessionContext?,
    ): Meldekort? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        select
                            *
                        from meldekort_bruker
                        where meldeperiode_id = :meldeperiode_id
                    """,
                    "meldeperiode_id" to meldeperiodeId,
                ).map { row ->
                    fromRow(row, session)
                }.asSingle,
            )
        }
    }

    /** TODO jah: Denne må returnere en liste dersom vi støtter flere innsender på samme meldeperiode */
    override fun hentMeldekortForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        sessionContext: SessionContext?,
    ): Meldekort? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select
                        mk.*
                    from meldekort_bruker mk
                    join meldeperiode mp on mk.meldeperiode_id = mp.id
                    where mp.kjede_id = :kjede_id
                    """,
                    "kjede_id" to kjedeId.verdi,
                ).map { row ->
                    fromRow(row, session)
                }.asSingle,
            )
        }
    }

    override fun hentForMeldekortId(
        meldekortId: MeldekortId,
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Meldekort? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select
                        mk.*
                    from meldekort_bruker mk
                    join meldeperiode mp on mp.id = mk.meldeperiode_id
                    where mk.id = :id 
                    and mp.fnr = :fnr
                    """,
                    "id" to meldekortId.toString(),
                    "fnr" to fnr.verdi,
                ).map { row ->
                    fromRow(row, session)
                }.asSingle,
            )
        }
    }

    /** Henter siste meldekort som kan utfylles eller er utfylt av bruker */
    override fun hentSisteMeldekortForBruker(fnr: Fnr, sessionContext: SessionContext?): Meldekort? {
        return this.hentMeldekortForBruker(fnr, 1, sessionContext).firstOrNull()
    }

    /** Henter neste meldekort som kan utfylles av bruker */
    override fun hentNesteMeldekortTilUtfylling(fnr: Fnr, sessionContext: SessionContext?): Meldekort? {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        select
                            mk.*
                        from meldekort_bruker mk
                        join meldeperiode mp on mp.fnr = :fnr
                        where mp.id = mk.meldeperiode_id and mk.mottatt is null and mp.til_og_med <= :maks_til_og_med
                        order by fra_og_med, versjon desc
                        limit 1
                    """,
                    "fnr" to fnr.verdi,
                    "maks_til_og_med" to senesteTilOgMedDatoForInnsending(),
                ).map { row -> fromRow(row, session) }.asSingle,
            )
        }
    }

    /** Henter alle meldekort som kan utfylles eller er utfylt */
    override fun hentAlleMeldekortForBruker(fnr: Fnr, sessionContext: SessionContext?): List<Meldekort> {
        return this.hentMeldekortForBruker(fnr, 100, sessionContext)
    }

    override fun hentMeldekortForSendingTilSaksbehandling(sessionContext: SessionContext?): List<Meldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """ 
                        select * from meldekort_bruker
                        where sendt_til_saksbehandling is null
                        and journalpost_id is not null
                        and mottatt is not null
                    """,
                )
                    .map { row -> fromRow(row, session) }.asList,
            )
        }
    }

    override fun markerSendtTilSaksbehandling(
        id: MeldekortId,
        sendtTidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """ 
                        update meldekort_bruker set
                            sendt_til_saksbehandling = :sendtTidspunkt
                        where id = :id
                    """,
                    "id" to id.toString(),
                    "sendtTidspunkt" to sendtTidspunkt,
                ).asUpdate,
            )
        }
    }

    private fun hentMeldekortForBruker(
        fnr: Fnr,
        limit: Int = 100,
        sessionContext: SessionContext?,
    ): List<Meldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        select
                            mk.*
                        from meldekort_bruker mk
                        join meldeperiode mp on mp.fnr = :fnr
                        where mp.id = mk.meldeperiode_id and mp.til_og_med <= :maks_til_og_med
                        order by fra_og_med desc, versjon desc
                        limit :limit
                    """,
                    "fnr" to fnr.verdi,
                    "limit" to limit,
                    "maks_til_og_med" to senesteTilOgMedDatoForInnsending(),
                ).map { row -> fromRow(row, session) }.asList,
            )
        }
    }

    override fun markerJournalført(
        meldekortId: MeldekortId,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                queryOf(
                    //language=sql
                    """
                      update meldekort_bruker
                      set journalpost_id = :journalpost_id,
                          journalføringstidspunkt = :tidspunkt
                      where id = :id
                    """.trimIndent(),
                    mapOf(
                        "id" to meldekortId.toString(),
                        "journalpost_id" to journalpostId.toString(),
                        "tidspunkt" to tidspunkt,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentDeSomSkalJournalføres(limit: Int, sessionContext: SessionContext?): List<Meldekort> =
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                queryOf(
                    //language=sql
                    """
                    select u.*, mp.fnr as fnr, mp.saksnummer 
                    from meldekort_bruker u 
                    join meldeperiode mp on u.meldeperiode_id = mp.id and mp.saksnummer is not null
                    where u.journalpost_id is null
                    and u.mottatt is not null
                    limit :limit
                    """.trimIndent(),
                    mapOf("limit" to limit),
                ).map { row ->
                    fromRow(row, session)
                }.asList,
            )
        }

    override fun hentDeSomIkkeHarBlittVarsletFor(limit: Int, sessionContext: SessionContext?): List<Meldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select * from meldekort_bruker mk
                    join meldeperiode mp on mp.id = mk.meldeperiode_id
                    where mp.til_og_med <= :maks_til_og_med
                    and mk.mottatt is null
                    and mk.varsel_inaktivert is false
                    limit :limit
                    """,
                    "limit" to limit,
                    "maks_til_og_med" to senesteTilOgMedDatoForInnsending(),
                ).map { row -> fromRow(row, session) }.asList,
            )
        }
    }
    override fun hentMottatteSomDetVarslesFor(limit: Int, sessionContext: SessionContext?): List<Meldekort> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                queryOf(
                    //language=sql
                    """
                    select * from meldekort_bruker
                    where mottatt is not null 
                    and varsel_id is not null
                    and varsel_inaktivert is false
                    limit :limit
                    """.trimIndent(),
                    mapOf("limit" to limit),
                ).map { row -> fromRow(row, session) }.asList,
            )
        }
    }

    companion object {
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
                sakId = SakId.fromString(row.string("sak_id")),
                dager = row.string("dager").toMeldekortDager(),
                journalpostId = row.stringOrNull("journalpost_id")?.let { JournalpostId(it) },
                journalføringstidspunkt = row.localDateTimeOrNull("journalføringstidspunkt"),
                varselId = row.stringOrNull("varsel_id")?.let { VarselId(it) },
                erVarselInaktivert = row.boolean("varsel_inaktivert"),
            )
        }
    }
}
