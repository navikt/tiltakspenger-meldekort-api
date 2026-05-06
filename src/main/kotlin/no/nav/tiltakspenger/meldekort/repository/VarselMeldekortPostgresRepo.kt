package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.varsler.BeskjedMeldeperiode
import no.nav.tiltakspenger.meldekort.domene.varsler.GirRettEndring
import no.nav.tiltakspenger.meldekort.domene.varsler.KjedeSomManglerInnsending
import no.nav.tiltakspenger.meldekort.domene.varsler.MeldeperiodeEndring
import no.nav.tiltakspenger.meldekort.domene.varsler.Verdiendring
import tools.jackson.core.type.TypeReference
import java.time.LocalDate

class VarselMeldekortPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : VarselMeldekortRepo {

    /**
     * For hver kjede i saken, finn nyeste meldeperiode-versjon.
     * En kjede "mangler innsending" dersom:
     * 1. Nyeste versjon har maks_antall_dager_for_periode > 0 (bruker har rett til å fylle ut minst én dag)
     * 2. Det ikke finnes et innsendt, ikke-deaktivert meldekort for noen versjon i kjeden.
     */
    override fun hentKjederSomManglerInnsending(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): List<KjedeSomManglerInnsending> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    WITH nyeste_meldeperiode_per_kjede AS (
                        SELECT DISTINCT ON (mp.kjede_id)
                            mp.id AS meldeperiode_id,
                            mp.kjede_id,
                            mp.versjon,
                            mp.sak_id,
                            mp.maks_antall_dager_for_periode,
                            mp.kan_fylles_ut_fra_og_med
                        FROM meldeperiode mp
                        WHERE mp.sak_id = :sak_id
                        ORDER BY mp.kjede_id, mp.versjon DESC
                    )
                    SELECT
                        nm.sak_id,
                        nm.meldeperiode_id,
                        nm.kjede_id,
                        nm.versjon,
                        nm.kan_fylles_ut_fra_og_med
                    FROM nyeste_meldeperiode_per_kjede nm
                    WHERE nm.maks_antall_dager_for_periode > 0
                    AND NOT EXISTS (
                        SELECT 1
                        FROM meldekort_bruker mk
                        JOIN meldeperiode innsendt_mp ON innsendt_mp.id = mk.meldeperiode_id
                        WHERE innsendt_mp.sak_id = nm.sak_id
                          AND innsendt_mp.kjede_id = nm.kjede_id
                          AND mk.mottatt IS NOT NULL
                          AND mk.deaktivert IS NULL
                    )
                    ORDER BY nm.kan_fylles_ut_fra_og_med
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row ->
                    KjedeSomManglerInnsending(
                        sakId = SakId.fromString(row.string("sak_id")),
                        meldeperiodeId = MeldeperiodeId.fromString(row.string("meldeperiode_id")),
                        kjedeId = MeldeperiodeKjedeId(row.string("kjede_id")),
                        nyesteVersjon = row.int("versjon"),
                        kanFyllesUtFraOgMed = row.localDateTime("kan_fylles_ut_fra_og_med"),
                    )
                }.asList,
            )
        }
    }

    override fun hentMeldeperioderSomSkalHaBeskjed(
        sakId: SakId,
        sessionContext: SessionContext?,
    ): List<BeskjedMeldeperiode> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    WITH nyeste_meldeperiode_per_kjede AS (
                        SELECT DISTINCT ON (mp.kjede_id)
                            mp.id AS meldeperiode_id,
                            mp.kjede_id,
                            mp.versjon,
                            mp.sak_id,
                            mp.maks_antall_dager_for_periode,
                            mp.gir_rett,
                            mp.kan_fylles_ut_fra_og_med
                        FROM meldeperiode mp
                        WHERE mp.sak_id = :sak_id
                        ORDER BY mp.kjede_id, mp.versjon DESC
                    ), siste_innsendte_meldeperiode_per_kjede AS (
                        SELECT DISTINCT ON (mp.kjede_id)
                            mp.id AS meldeperiode_id,
                            mp.kjede_id,
                            mp.versjon,
                            mp.sak_id,
                            mp.maks_antall_dager_for_periode,
                            mp.gir_rett
                        FROM meldeperiode mp
                        JOIN meldekort_bruker mk ON mk.meldeperiode_id = mp.id
                        WHERE mp.sak_id = :sak_id
                          AND mk.mottatt IS NOT NULL
                          AND mk.deaktivert IS NULL
                        ORDER BY mp.kjede_id, mp.versjon DESC
                    )
                    SELECT
                        ny.sak_id,
                        ny.meldeperiode_id,
                        ny.kjede_id,
                        ny.versjon,
                        ny.maks_antall_dager_for_periode,
                        ny.gir_rett,
                        innsendt.versjon AS siste_innsendte_versjon,
                        innsendt.maks_antall_dager_for_periode AS siste_innsendte_maks_antall_dager_for_periode,
                        innsendt.gir_rett AS siste_innsendte_gir_rett
                    FROM nyeste_meldeperiode_per_kjede ny
                    JOIN siste_innsendte_meldeperiode_per_kjede innsendt
                      ON innsendt.sak_id = ny.sak_id
                     AND innsendt.kjede_id = ny.kjede_id
                    WHERE ny.versjon > innsendt.versjon
                      AND NOT EXISTS (
                          SELECT 1
                          FROM beskjed_varsel_meldeperiode bvm
                          WHERE bvm.sak_id = ny.sak_id
                            AND bvm.kjede_id = ny.kjede_id
                            AND bvm.versjon = ny.versjon
                      )
                    ORDER BY ny.kan_fylles_ut_fra_og_med
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row ->
                    val sisteInnsendteMaksAntallDager = row.int("siste_innsendte_maks_antall_dager_for_periode")
                    val nyesteMaksAntallDager = row.int("maks_antall_dager_for_periode")
                    val sisteInnsendteGirRett = row.string("siste_innsendte_gir_rett").fromDbJsonToGirRett()
                    val nyesteGirRett = row.string("gir_rett").fromDbJsonToGirRett()
                    val maksAntallDagerEndring = if (sisteInnsendteMaksAntallDager == nyesteMaksAntallDager) {
                        null
                    } else {
                        Verdiendring(fra = sisteInnsendteMaksAntallDager, til = nyesteMaksAntallDager)
                    }
                    val girRettEndringer = nyesteGirRett.diffFra(sisteInnsendteGirRett)
                    if (maksAntallDagerEndring == null && girRettEndringer.isEmpty()) {
                        return@map null
                    }
                    BeskjedMeldeperiode(
                        sakId = SakId.fromString(row.string("sak_id")),
                        meldeperiodeId = MeldeperiodeId.fromString(row.string("meldeperiode_id")),
                        kjedeId = MeldeperiodeKjedeId(row.string("kjede_id")),
                        versjon = row.int("versjon"),
                        sisteInnsendteVersjon = row.int("siste_innsendte_versjon"),
                        endring = MeldeperiodeEndring(
                            maksAntallDagerForPeriode = maksAntallDagerEndring,
                            girRett = girRettEndringer,
                        ),
                    )
                }.asList,
            ).filterNotNull()
        }
    }

    private fun String.fromDbJsonToGirRett(): Map<LocalDate, Boolean> {
        val typeRef = object : TypeReference<Map<LocalDate, Boolean>>() {}
        return objectMapper.readValue(this, typeRef)
    }

    private fun Map<LocalDate, Boolean>.diffFra(forrige: Map<LocalDate, Boolean>): List<GirRettEndring> {
        require(keys == forrige.keys) {
            "Kan ikke sammenligne girRett for meldeperioder med ulike datoer"
        }
        return entries.mapNotNull { (dato, nyVerdi) ->
            val forrigeVerdi = forrige.getValue(dato)
            if (forrigeVerdi == nyVerdi) {
                null
            } else {
                GirRettEndring(dato = dato, fra = forrigeVerdi, til = nyVerdi)
            }
        }
    }
}
