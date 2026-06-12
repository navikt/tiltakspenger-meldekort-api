package no.nav.tiltakspenger.meldekort.microfrontend.infra.repo

import arrow.core.Either
import kotliquery.Row
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendFeil
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendMeldekortInfo
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendRepo
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendSak
import java.time.Clock

class MicrofrontendPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : MicrofrontendRepo {

    override fun oppdaterStatusForMicrofrontend(
        sakId: SakId,
        aktiv: Boolean,
        sessionContext: SessionContext?,
    ): Either<MicrofrontendFeil, Unit> = Either.catch {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    update sak set
                        microfrontend_status = :microfrontend_status
                    where id = :id
                    """,
                    "id" to sakId.toString(),
                    "microfrontend_status" to if (aktiv) MicrofrontendStatusDb.AKTIV.toString() else MicrofrontendStatusDb.INAKTIV.toString(),
                ).asUpdate,
            )
        }
        Unit
    }.mapLeft { MicrofrontendFeil.DatabaseFeil(it) }

    /**
     * TODO jah: Rett opp i egen PR med tester+fakers. 1) offset er en duration og ikke et tidspunkt 2) aktivStatus trenger ikke være parameterisert 3) bruk antall_dager > 0 4) tar ikke høyde for stans/opphør 5) bruker ikke siste versjon av meldeperioden 6) bruker Skriv om til en liste med (fnr + sakId)-par. 7) Finn ut hvordan dette skal fungere, sett deg ned med fag.
     */
    override fun hentSakerHvorMicrofrontendSkalAktiveres(limit: Int, sessionContext: SessionContext?): Either<MicrofrontendFeil, List<MicrofrontendSak>> = Either.catch {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        SELECT s.id, s.fnr
                        FROM sak s
                        WHERE s.microfrontend_status <> :aktivStatus
                          AND EXISTS (
                              SELECT 1
                              FROM meldeperiode m
                              WHERE m.sak_id = s.id
                                AND (
                                    EXISTS (
                                        SELECT 1
                                        FROM jsonb_each_text(m.gir_rett) kv(key, value)
                                        WHERE value::boolean
                                    )
                                    AND (
                                        m.opprettet > :offset
                                        OR m.til_og_med > :offset
                                    )
                                )
                          )
                        LIMIT :limit;
                    """.trimIndent(),
                    "offset" to nå(clock).minusMonths(1),
                    "aktivStatus" to MicrofrontendStatusDb.AKTIV.toString(),
                    "limit" to limit,
                ).map { row -> fromRow(row) }.asList,
            )
        }
    }.mapLeft { MicrofrontendFeil.DatabaseFeil(it) }

    /**
     * TODO jah: Rett opp i egen PR med tester+fakers. 1) offset er en duration og ikke et tidspunkt 2) aktivStatus trenger ikke være parameterisert 3) bruk antall_dager > 0 4) tar ikke høyde for stans/opphør 5) bruker ikke siste versjon av meldeperioden 6) bruker Skriv om til en liste med (fnr + sakId)-par. 7) Finn ut hvordan dette skal fungere, sett deg ned med fag.
     */
    override fun hentSakerHvorMicrofrontendSkalInaktiveres(limit: Int, sessionContext: SessionContext?): Either<MicrofrontendFeil, List<MicrofrontendSak>> = Either.catch {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        SELECT s.id, s.fnr
                        FROM sak s
                        WHERE s.microfrontend_status <> :inaktivStatus
                          AND NOT EXISTS (
                            SELECT 1
                            FROM meldeperiode m
                            WHERE m.sak_id = s.id
                              AND (
                                EXISTS (
                                    SELECT 1
                                    FROM jsonb_each_text(m.gir_rett) kv(key, value)
                                    WHERE value::boolean
                                )
                                AND (
                                    m.opprettet > :offset
                                    OR m.til_og_med > :offset
                                )
                              )
                          )
                        LIMIT :limit;
                    """.trimIndent(),
                    "offset" to nå(clock).minusMonths(1),
                    "inaktivStatus" to MicrofrontendStatusDb.INAKTIV.toString(),
                    "limit" to limit,
                ).map { row -> fromRow(row) }.asList,
            )
        }
    }.mapLeft { MicrofrontendFeil.DatabaseFeil(it) }

    /**
     * Henter info microfrontend trenger om brukers meldekort i én spørring:
     * antall meldekort klare til innsending nå, og tidspunktet neste meldekort til utfylling kan sendes inn.
     *
     * OBS! Filtreringen dupliserer logikken i
     * [no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortPostgresRepo.hentAlleMeldekortKlarTilInnsending]
     * og [no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortPostgresRepo.hentNesteMeldekortTilUtfylling].
     * Microfrontend eier sin egen spørring bevisst (isolasjon), men endres logikken ett sted bør den endres begge steder.
     */
    override fun hentMeldekortInfo(fnr: Fnr, sessionContext: SessionContext?): Either<MicrofrontendFeil, MicrofrontendMeldekortInfo> = Either.catch {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        WITH kandidater AS MATERIALIZED (
                            SELECT
                                mp.kan_fylles_ut_fra_og_med,
                                mp.fra_og_med,
                                mp.versjon
                            FROM meldekort_bruker mb
                            JOIN meldeperiode mp ON mp.id = mb.meldeperiode_id
                            JOIN sak s ON s.id = mp.sak_id
                            WHERE s.fnr = :fnr
                              AND mb.mottatt IS NULL
                              AND mb.deaktivert IS NULL
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM meldeperiodebehandling mb_vedtak
                                  WHERE mb_vedtak.sak_id = mp.sak_id
                                    AND mb_vedtak.meldeperiode_kjede_id = mp.kjede_id
                              )
                        )
                        SELECT
                            (
                                SELECT count(*)
                                FROM kandidater
                                WHERE kan_fylles_ut_fra_og_med <= :tidsgrense
                            ) AS antall_klar_til_innsending,
                            (
                                SELECT kan_fylles_ut_fra_og_med
                                FROM kandidater
                                ORDER BY fra_og_med, versjon DESC
                                LIMIT 1
                            ) AS neste_mulige_innsendingstidspunkt
                    """.trimIndent(),
                    "fnr" to fnr.verdi,
                    "tidsgrense" to nå(clock),
                ).map { row ->
                    MicrofrontendMeldekortInfo(
                        antallMeldekortKlarTilInnsending = row.int("antall_klar_til_innsending"),
                        nesteMuligeInnsendingstidspunkt = row.localDateTimeOrNull("neste_mulige_innsendingstidspunkt"),
                    )
                }.asSingle,
            )!!
        }
    }.mapLeft { MicrofrontendFeil.DatabaseFeil(it) }

    companion object {
        private fun fromRow(row: Row): MicrofrontendSak = MicrofrontendSak(
            sakId = SakId.fromString(row.string("id")),
            fnr = Fnr.fromString(row.string("fnr")),
        )
    }
}
