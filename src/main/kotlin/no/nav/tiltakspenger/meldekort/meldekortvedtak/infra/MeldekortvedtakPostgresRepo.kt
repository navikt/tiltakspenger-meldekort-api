package no.nav.tiltakspenger.meldekort.meldekortvedtak.infra

import kotliquery.Row
import kotliquery.Session
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldeperiodebehandling
import java.time.LocalDateTime

/**
 * Lesesiden for meldekortvedtak (CQRS).
 *
 * Skrivesiden (INSERT) bor i [no.nav.tiltakspenger.meldekort.mottak.infra.MottakPostgresRepo.lagreMeldekortvedtak].
 * Endres skjemaet for `meldekortvedtak`- eller `meldeperiodebehandling`-tabellen må begge stedene oppdateres.
 */
object MeldekortvedtakPostgresRepo {

    fun hentForSakId(sakId: SakId, session: Session): List<Meldekortvedtak> {
        // Ett meldekortvedtak har én eller flere meldeperiodebehandlinger.
        // Vi joiner og grupperer i Kotlin.
        // Radene materialiseres fullt ut inne i .map (til Rad) før ResultSet lukkes - hentForSakId kalles nestet
        // inne i SakPostgresRepo.fromRow sin egen åpne ResultSet, så vi kan ikke holde på Row-referanser.
        val rader = session.run(
            sqlQuery(
                """
                SELECT
                    mv.id,
                    mv.opprettet,
                    mv.er_korrigering,
                    mv.er_automatisk_behandlet,
                    mb.meldeperiode_id,
                    mb.meldeperiode_kjede_id,
                    mb.brukers_meldekort_id,
                    mb.fra_og_med,
                    mb.til_og_med,
                    mb.dager
                FROM meldekortvedtak mv
                JOIN meldeperiodebehandling mb ON mb.meldekortvedtak_id = mv.id
                WHERE mv.sak_id = :sak_id
                ORDER BY mv.opprettet, mb.fra_og_med
                """,
                "sak_id" to sakId.toString(),
            ).map { row -> row.toRad() }.asList,
        )

        return rader
            .groupBy { it.vedtakId }
            .map { (vedtakId, raderForVedtak) ->
                val første = raderForVedtak.first()
                Meldekortvedtak(
                    id = vedtakId,
                    sakId = sakId,
                    opprettet = første.opprettet,
                    erKorrigering = første.erKorrigering,
                    erAutomatiskBehandlet = første.erAutomatiskBehandlet,
                    meldeperiodebehandlinger = raderForVedtak.map { it.behandling },
                )
            }
    }

    private data class Rad(
        val vedtakId: VedtakId,
        val opprettet: LocalDateTime,
        val erKorrigering: Boolean,
        val erAutomatiskBehandlet: Boolean,
        val behandling: Meldeperiodebehandling,
    )

    private fun Row.toRad(): Rad = Rad(
        vedtakId = VedtakId.fromString(string("id")),
        opprettet = localDateTime("opprettet"),
        erKorrigering = boolean("er_korrigering"),
        erAutomatiskBehandlet = boolean("er_automatisk_behandlet"),
        behandling = Meldeperiodebehandling(
            meldeperiodeId = MeldeperiodeId.fromString(string("meldeperiode_id")),
            meldeperiodeKjedeId = MeldeperiodeKjedeId(string("meldeperiode_kjede_id")),
            brukersMeldekortId = stringOrNull("brukers_meldekort_id")?.let { MeldekortId.fromString(it) },
            periode = Periode(
                fraOgMed = localDate("fra_og_med"),
                tilOgMed = localDate("til_og_med"),
            ),
            dager = string("dager").tilMeldeperiodebehandlingDager(),
        ),
    )
}
