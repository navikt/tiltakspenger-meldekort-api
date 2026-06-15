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
     * En sak skal aktiveres når den ikke allerede er aktiv og bruker har en meldeperiodekjede som mangler innsending (se [HAR_KJEDE_SOM_MANGLER_INNSENDING]).
     */
    override fun hentSakerHvorMicrofrontendSkalAktiveres(limit: Int, sessionContext: SessionContext?): Either<MicrofrontendFeil, List<MicrofrontendSak>> = Either.catch {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        SELECT s.id, s.fnr
                        FROM sak s
                        WHERE s.microfrontend_status <> '${MicrofrontendStatusDb.AKTIV}'
                          AND EXISTS ($HAR_KJEDE_SOM_MANGLER_INNSENDING)
                        LIMIT :limit;
                    """.trimIndent(),
                    "limit" to limit,
                ).map { row -> fromRow(row) }.asList,
            )
        }
    }.mapLeft { MicrofrontendFeil.DatabaseFeil(it) }

    /**
     * En sak skal inaktiveres når den ikke allerede er inaktiv og bruker ikke (lenger) har en meldeperiodekjede som mangler innsending (se [HAR_KJEDE_SOM_MANGLER_INNSENDING]).
     */
    override fun hentSakerHvorMicrofrontendSkalInaktiveres(limit: Int, sessionContext: SessionContext?): Either<MicrofrontendFeil, List<MicrofrontendSak>> = Either.catch {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                        SELECT s.id, s.fnr
                        FROM sak s
                        WHERE s.microfrontend_status <> '${MicrofrontendStatusDb.INAKTIV}'
                          AND NOT EXISTS ($HAR_KJEDE_SOM_MANGLER_INNSENDING)
                        LIMIT :limit;
                    """.trimIndent(),
                    "limit" to limit,
                ).map { row -> fromRow(row) }.asList,
            )
        }
    }.mapLeft { MicrofrontendFeil.DatabaseFeil(it) }

    /**
     * Henter info microfrontend trenger om brukers meldekort i én spørring: antall meldekort klare til innsending nå, og tidspunktet neste meldekort til utfylling kan sendes inn.
     *
     * Merk skillet mellom *om* kortet vises og *hva* det viser: en meldeperiode som gir rett, men som ennå ikke kan fylles ut (`kan_fylles_ut_fra_og_med` fram i tid), holder kortet åpent (se [HAR_KJEDE_SOM_MANGLER_INNSENDING], som med vilje ikke har noe tidsfilter).
     * Da blir `antall_klar_til_innsending` = 0 (teller kun de som kan sendes inn nå), mens `neste_mulige_innsendingstidspunkt` returnerer det framtidige tidspunktet – slik at brukeren ser «Neste meldekort kan sendes inn \<dato\>».
     * Dette er bevisst: kortet står åpent helt til samtlige meldeperiodekjeder er fylt ut (av bruker eller saksbehandler) eller ikke lenger gir rett.
     *
     * OBS! Filtreringen dupliserer logikken i [no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortPostgresRepo.hentAlleMeldekortKlarTilInnsending] og [no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortPostgresRepo.hentNesteMeldekortTilUtfylling].
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
        /**
         * EXISTS/NOT EXISTS-predikat som er sant når en sak fortsatt har en meldeperiodekjede som "mangler innsending", dvs. en åpen meldekort-oppgave brukeren bør gjøre noe med.
         *
         * Dette speiler bevisst varsler-pakken sin [no.nav.tiltakspenger.meldekort.varsler.infra.VarselMeldekortPostgresRepo.hentFørsteKjedeSomManglerInnsending], slik at microfrontend-kortet og meldekort-varslene (som vises nær hverandre på «Min side») henger sammen.
         * En kjede mangler innsending når den *siste versjonen* av kjeden:
         *  - gir rett til å fylle ut minst én dag (`maks_antall_dager_for_periode > 0`),
         *  - ikke har et mottatt brukermeldekort på noen meldeperiode i kjeden (`meldekort_bruker.mottatt`), og
         *  - ikke har en `meldeperiodebehandling` for kjeden (saksbehandler-innsending, f.eks. papirmeldekort).
         *
         * Det finnes bevisst *ingen* tidsgrense / gyldighetsvindu: kortet vises så lenge oppgaven står åpen (bruker kan miste penger ved å ikke fylle ut).
         * Dette skiller seg litt fra varslene, som fjernes etter 1 år (logikk som minside eier).
         * Kortet står altså åpent helt til *samtlige* meldeperiodekjeder enten er fylt ut (av bruker eller saksbehandler) eller ikke lenger gir rett (`maks_antall_dager_for_periode = 0`).
         * Dette gjelder også kjeder som ennå ikke kan fylles ut (framtidig `kan_fylles_ut_fra_og_med`) – de holder kortet åpent, og innholdet viser «neste mulige innsendingstidspunkt» (se [hentMeldekortInfo]).
         * Utdaterings-/opprydningslogikk kan eventuelt legges på senere.
         */
        private val HAR_KJEDE_SOM_MANGLER_INNSENDING =
            """
            SELECT 1
            FROM meldeperiode m
            WHERE m.sak_id = s.id
              AND NOT EXISTS (
                  SELECT 1
                  FROM meldeperiode nyere
                  WHERE nyere.sak_id = m.sak_id
                    AND nyere.kjede_id = m.kjede_id
                    AND nyere.versjon > m.versjon
              )
              AND m.maks_antall_dager_for_periode > 0
              AND NOT EXISTS (
                  SELECT 1
                  FROM meldekort_bruker mk
                  JOIN meldeperiode mk_mp ON mk_mp.id = mk.meldeperiode_id
                  WHERE mk_mp.sak_id = m.sak_id
                    AND mk_mp.kjede_id = m.kjede_id
                    AND mk.mottatt IS NOT NULL
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM meldeperiodebehandling mpb
                  WHERE mpb.sak_id = m.sak_id
                    AND mpb.meldeperiode_kjede_id = m.kjede_id
              )
            """.trimIndent()

        private fun fromRow(row: Row): MicrofrontendSak = MicrofrontendSak(
            sakId = SakId.fromString(row.string("id")),
            fnr = Fnr.fromString(row.string("fnr")),
        )
    }
}
