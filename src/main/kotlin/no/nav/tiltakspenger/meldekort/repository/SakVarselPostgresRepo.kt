package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.varsler.SakForVarselvurdering
import java.time.LocalDateTime

class SakVarselPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
) : SakVarselRepo {

    override fun flaggForVarselvurdering(
        sakId: SakId,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    // clock_timestamp() sikrer en ny verdi per kall (ulikt now()/
                    // transaction_timestamp() som er konstant innen en transaksjon), slik at
                    // den optimistiske låsen i markerVarselVurdert oppdager samtidige flagginger.
                    """
                    UPDATE sak
                    SET skal_vurdere_varsel = true,
                        sist_flagget_tidspunkt = clock_timestamp()
                    WHERE id = :id
                    """,
                    "id" to sakId.toString(),
                ).asUpdate,
            )
        }
    }

    override fun hentSakerSomSkalVurdereVarsel(
        limit: Int,
        sessionContext: SessionContext?,
    ): List<SakForVarselvurdering> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                // Henter kun de feltene VurderVarselService trenger. Vi henter spesielt IKKE
                // meldeperioder her (en N+1-spørring ville bli kjørt hvert 10. sek). Sortering
                // på sist_flagget_tidspunkt NULLS FIRST gir oss "first flagged, first served" –
                // saker som har stått lengst i kø prioriteres, og ingen sak kan sulte. NULLS
                // FIRST plukker også opp saker som ikke er flagget eksplisitt enda (f.eks.
                // ved første kjøring etter migrering).
                sqlQuery(
                    """
                    SELECT id, saksnummer, fnr, sist_flagget_tidspunkt
                    FROM sak
                    WHERE skal_vurdere_varsel = true
                    ORDER BY sist_flagget_tidspunkt NULLS FIRST, id
                    LIMIT :limit
                    """,
                    "limit" to limit,
                ).map { row ->
                    SakForVarselvurdering(
                        sakId = SakId.fromString(row.string("id")),
                        saksnummer = row.string("saksnummer"),
                        fnr = Fnr.fromString(row.string("fnr")),
                        sistFlaggetTidspunkt = row.localDateTimeOrNull("sist_flagget_tidspunkt"),
                    )
                }.asList,
            )
        }
    }

    override fun markerVarselVurdert(
        sakId: SakId,
        vurdertTidspunkt: LocalDateTime,
        sistFlaggetTidspunktVedLesing: LocalDateTime?,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            val antallOppdatert = session.run(
                sqlQuery(
                    // Optimistisk lås: oppdater kun hvis sist_flagget_tidspunkt ikke har endret
                    // seg siden jobben leste saken. IS NOT DISTINCT FROM håndterer NULL korrekt
                    // (NULL IS NOT DISTINCT FROM NULL = true). Hvis tidspunktet er endret har en
                    // konkurrerende transaksjon flagget saken på nytt (typisk mottak av nytt
                    // meldekort). Vi kaster OptimistiskLåsFeil slik at varseljobbens transaksjon
                    // rulles tilbake, og saken plukkes opp på nytt i neste kjøring med oppdatert
                    // datagrunnlag.
                    """
                    UPDATE sak SET
                        skal_vurdere_varsel = false,
                        sist_vurdert_varsel = :vurdert_tidspunkt
                    WHERE id = :id
                      AND sist_flagget_tidspunkt IS NOT DISTINCT FROM :sist_flagget_tidspunkt
                    """,
                    "id" to sakId.toString(),
                    "vurdert_tidspunkt" to vurdertTidspunkt,
                    "sist_flagget_tidspunkt" to sistFlaggetTidspunktVedLesing,
                ).asUpdate,
            )
            if (antallOppdatert == 0) {
                throw OptimistiskLåsFeil(
                    "Optimistisk lås slo til for sak $sakId: sist_flagget_tidspunkt er endret siden vi leste saken. " +
                        "Varseljobben feiler og saken plukkes opp på nytt i neste kjøring.",
                )
            }
        }
    }
}

/**
 * Kastes av [SakVarselPostgresRepo.markerVarselVurdert] når sist_flagget_tidspunkt har endret
 * seg siden varseljobben leste saken. Signaliserer at hele varseltransaksjonen skal rulles
 * tilbake og saken vurderes på nytt i neste kjøring.
 */
class OptimistiskLåsFeil(message: String) : RuntimeException(message)
