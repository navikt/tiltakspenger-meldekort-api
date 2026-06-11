package no.nav.tiltakspenger.meldekort.mottak.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.TransactionalSession
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.arena.infra.tilDb
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldeperiodebehandling
import no.nav.tiltakspenger.meldekort.meldekortvedtak.infra.tilDagerDbJson
import no.nav.tiltakspenger.meldekort.meldekortvedtak.infra.tilMeldeperiodebehandlingerDbJson
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.mottak.MottakRepo
import no.nav.tiltakspenger.meldekort.mottak.MottattSak
import java.time.LocalDate

/**
 * Skrivesiden for mottak fra saksbehandling-api (CQRS): INSERT/UPDATE av sak, meldeperiode og meldekortvedtak.
 *
 * Lesesiden for meldekortvedtak (SELECT) bor i [no.nav.tiltakspenger.meldekort.meldekortvedtak.infra.MeldekortvedtakPostgresRepo].
 * Endres skjemaet for `meldekortvedtak`- eller `meldeperiodebehandling`-tabellen må begge stedene oppdateres.
 */
class MottakPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : MottakRepo {

    private val logger = KotlinLogging.logger {}

    override fun lagreSak(
        sak: MottattSak,
        transactionContext: TransactionContext,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                    insert into sak (
                        id,
                        saksnummer,
                        fnr,
                        arena_meldekort_status,
                        har_soknad_under_behandling,
                        kan_sende_inn_helg_for_meldekort
                    ) values (
                        :id,
                        :saksnummer,
                        :fnr,
                        :arena_meldekort_status,
                        :har_soknad_under_behandling,
                        :kan_sende_inn_helg_for_meldekort
                    )
                    """,
                    "id" to sak.id.toString(),
                    "saksnummer" to sak.saksnummer,
                    "fnr" to sak.fnr.verdi,
                    // En nylig mottatt sak har ennå ukjent arena-status; den settes/oppdateres av egen jobb.
                    "arena_meldekort_status" to ArenaMeldekortStatus.UKJENT.tilDb(),
                    "har_soknad_under_behandling" to sak.harSoknadUnderBehandling,
                    "kan_sende_inn_helg_for_meldekort" to sak.kanSendeInnHelgForMeldekort,
                ).asUpdate,
            )
        }
    }

    /** Oppdaterer fnr, søknadsbehandlingstatus, og kanSendeInnHelgForMeldekort på en sak */
    override fun oppdaterSak(
        sak: MottattSak,
        transactionContext: TransactionContext,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                    update sak set 
                        fnr = :fnr,
                        har_soknad_under_behandling = :har_soknad_under_behandling,
                        kan_sende_inn_helg_for_meldekort = :kan_sende_inn_helg_for_meldekort
                    where id = :id
                    """,
                    "id" to sak.id.toString(),
                    "fnr" to sak.fnr.verdi,
                    "har_soknad_under_behandling" to sak.harSoknadUnderBehandling,
                    "kan_sende_inn_helg_for_meldekort" to sak.kanSendeInnHelgForMeldekort,
                ).asUpdate,
            )
        }
    }

    override fun lagreMeldekortvedtak(
        meldekortvedtak: Meldekortvedtak,
        transactionContext: TransactionContext,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            // Meldekortvedtak er immutable etter iverksettelse og dedupliseres på id, jf. V37__meldekortvedtak.sql.
            // ON CONFLICT DO NOTHING gjør lagring idempotent og trygt ved samtidighet mellom PODs / retries fra innsender.
            //
            // Expand-fase (expand/contract for rullende deploy): vi dual-skriver behandlingene både til den
            // gamle JSONB-kolonnen `meldeperiodebehandlinger` (som gamle podder fortsatt leser) og til den nye
            // `meldeperiodebehandling`-tabellen. Når lesing er flyttet og gamle podder er ute, fjernes JSONB-skrivingen
            // og deretter selve kolonnen (egen migrering).
            val antallRader = tx.run(
                sqlQuery(
                    """
                    INSERT INTO meldekortvedtak (
                        id,
                        sak_id,
                        opprettet,
                        er_korrigering,
                        er_automatisk_behandlet,
                        meldeperiodebehandlinger
                    ) VALUES (
                        :id,
                        :sak_id,
                        :opprettet,
                        :er_korrigering,
                        :er_automatisk_behandlet,
                        to_jsonb(:meldeperiodebehandlinger::jsonb)
                    )
                    ON CONFLICT (id) DO NOTHING
                    """,
                    "id" to meldekortvedtak.id.toString(),
                    "sak_id" to meldekortvedtak.sakId.toString(),
                    "opprettet" to meldekortvedtak.opprettet,
                    "er_korrigering" to meldekortvedtak.erKorrigering,
                    "er_automatisk_behandlet" to meldekortvedtak.erAutomatiskBehandlet,
                    "meldeperiodebehandlinger" to meldekortvedtak.meldeperiodebehandlinger.tilMeldeperiodebehandlingerDbJson(),
                ).asUpdate,
            )
            if (antallRader == 0) {
                logger.info { "Hoppet over lagring av meldekortvedtak ${meldekortvedtak.id} - finnes allerede (sak ${meldekortvedtak.sakId})" }
            } else {
                // Behandlingene lagres kun når selve vedtaket faktisk ble satt inn, slik at lagringen forblir idempotent.
                meldekortvedtak.meldeperiodebehandlinger.forEach { behandling ->
                    lagreMeldeperiodebehandling(meldekortvedtak, behandling, tx)
                }
                logger.info { "Lagret meldekortvedtak ${meldekortvedtak.id} for sak ${meldekortvedtak.sakId}" }
            }
        }
    }

    private fun lagreMeldeperiodebehandling(
        meldekortvedtak: Meldekortvedtak,
        behandling: Meldeperiodebehandling,
        tx: TransactionalSession,
    ) {
        tx.run(
            sqlQuery(
                """
                INSERT INTO meldeperiodebehandling (
                    meldekortvedtak_id,
                    sak_id,
                    meldeperiode_id,
                    meldeperiode_kjede_id,
                    brukers_meldekort_id,
                    fra_og_med,
                    til_og_med,
                    dager
                ) VALUES (
                    :meldekortvedtak_id,
                    :sak_id,
                    :meldeperiode_id,
                    :meldeperiode_kjede_id,
                    :brukers_meldekort_id,
                    :fra_og_med,
                    :til_og_med,
                    to_jsonb(:dager::jsonb)
                )
                """,
                "meldekortvedtak_id" to meldekortvedtak.id.toString(),
                "sak_id" to meldekortvedtak.sakId.toString(),
                "meldeperiode_id" to behandling.meldeperiodeId.toString(),
                "meldeperiode_kjede_id" to behandling.meldeperiodeKjedeId.toString(),
                "brukers_meldekort_id" to behandling.brukersMeldekortId?.toString(),
                "fra_og_med" to behandling.periode.fraOgMed,
                "til_og_med" to behandling.periode.tilOgMed,
                "dager" to behandling.dager.tilDagerDbJson(),
            ).asUpdate,
        )
    }

    override fun lagreMeldeperiode(
        meldeperiode: Meldeperiode,
        transactionContext: TransactionContext,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx.run(
                sqlQuery(
                    """
                    insert into meldeperiode (
                        id,
                        kjede_id,
                        versjon,
                        sak_id,
                        opprettet,
                        fra_og_med,
                        til_og_med,
                        maks_antall_dager_for_periode,
                        gir_rett,
                        kan_fylles_ut_fra_og_med
                    ) values (
                        :id,
                        :kjede_id,
                        :versjon,
                        :sak_id,
                        :opprettet,
                        :fra_og_med,
                        :til_og_med,
                        :maks_antall_dager_for_periode,
                        to_jsonb(:gir_rett::jsonb),
                        :kan_fylles_ut_fra_og_med
                    )
                    """,
                    "id" to meldeperiode.id.toString(),
                    "kjede_id" to meldeperiode.kjedeId.toString(),
                    "versjon" to meldeperiode.versjon,
                    "sak_id" to meldeperiode.sakId.toString(),
                    "opprettet" to meldeperiode.opprettet,
                    "fra_og_med" to meldeperiode.periode.fraOgMed,
                    "til_og_med" to meldeperiode.periode.tilOgMed,
                    "maks_antall_dager_for_periode" to meldeperiode.maksAntallDagerForPeriode,
                    "gir_rett" to meldeperiode.girRett.toDbJson(),
                    "kan_fylles_ut_fra_og_med" to meldeperiode.kanFyllesUtFraOgMed,
                ).asUpdate,
            )
        }
    }

    private fun Map<LocalDate, Boolean>.toDbJson(): String {
        return entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ",",
        ) { (date, value) ->
            "\"${date}\": $value"
        }
    }
}
