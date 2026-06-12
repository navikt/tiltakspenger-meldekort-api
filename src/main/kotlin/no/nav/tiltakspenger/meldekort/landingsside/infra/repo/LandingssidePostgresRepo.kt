package no.nav.tiltakspenger.meldekort.landingsside.infra.repo

import kotliquery.Session
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.arena.infra.tilArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideMeldekort
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideRepo
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideSak
import java.time.Clock

class LandingssidePostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : LandingssideRepo {

    override fun hentSak(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): LandingssideSak? {
        return sessionFactory.withSession(sessionContext) { session ->
            // Slår opp saken én gang på fnr (unik indeks). Resten av spørringene filtrerer direkte på
            // sak_id (indeksert) og slipper dermed å joine mot sak igjen.
            // Hvis denne er null har vi ingen sak for denne brukeren i ny eller gammel løsning.
            val sakRad = hentSakRad(fnr, session) ?: return@withSession null

            LandingssideSak(
                fnr = fnr,
                arenaMeldekortStatus = sakRad.arenaMeldekortStatus,
                harInnsendteMeldekort = harInnsendteMeldekort(sakRad.sakId, session),
                meldekortTilUtfylling = hentMeldekortTilUtfylling(sakRad.sakId, session, clock),
            )
        }
    }
}

private data class SakRad(
    val sakId: SakId,
    val arenaMeldekortStatus: ArenaMeldekortStatus,
)

private fun hentSakRad(
    fnr: Fnr,
    session: Session,
): SakRad? {
    return session.run(
        sqlQuery(
            "SELECT id, arena_meldekort_status FROM sak WHERE fnr = :fnr",
            "fnr" to fnr.verdi,
        ).map { row ->
            SakRad(
                sakId = SakId.fromString(row.string("id")),
                arenaMeldekortStatus = row.string("arena_meldekort_status").tilArenaMeldekortStatus(),
            )
        }.asSingle,
    )
}

private fun harInnsendteMeldekort(
    sakId: SakId,
    session: Session,
): Boolean {
    return session.run(
        sqlQuery(
            """
        SELECT EXISTS (
            SELECT 1
            FROM meldekort_bruker mb
            WHERE mb.sak_id = :sak_id
              AND mb.mottatt IS NOT NULL
            UNION ALL
            SELECT 1
            FROM meldeperiodebehandling mb_vedtak
            WHERE mb_vedtak.sak_id = :sak_id
        ) AS har_innsendte_meldekort
        """,
            "sak_id" to sakId.toString(),
        ).map { row -> row.boolean("har_innsendte_meldekort") }.asSingle,
    ) == true
}

private fun hentMeldekortTilUtfylling(
    sakId: SakId,
    session: Session,
    clock: Clock,
): List<LandingssideMeldekort> {
    return session.run(
        sqlQuery(
            """
        SELECT mp.kan_fylles_ut_fra_og_med
        FROM meldekort_bruker mb
        JOIN meldeperiode mp ON mp.id = mb.meldeperiode_id
        WHERE mb.sak_id = :sak_id
          AND mb.mottatt IS NULL
          AND mb.deaktivert IS NULL
          AND mp.kan_fylles_ut_fra_og_med <= :tidsgrense
          AND NOT EXISTS (
              SELECT 1
              FROM meldeperiodebehandling mb_vedtak
              WHERE mb_vedtak.sak_id = mp.sak_id
                AND mb_vedtak.meldeperiode_kjede_id = mp.kjede_id
          )
        ORDER BY mp.kan_fylles_ut_fra_og_med
        """,
            "sak_id" to sakId.toString(),
            "tidsgrense" to nå(clock),
        ).map { row ->
            LandingssideMeldekort(
                kanSendesFra = row.localDateTime("kan_fylles_ut_fra_og_med"),
            )
        }.asList,
    )
}
