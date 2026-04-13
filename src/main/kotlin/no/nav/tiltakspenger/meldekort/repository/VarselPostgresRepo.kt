package no.nav.tiltakspenger.meldekort.repository

import kotliquery.Row
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel.Aktiv
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel.Inaktivert
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel.SkalAktiveres
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel.SkalInaktiveres
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsler
import java.time.Clock
import java.time.LocalDateTime

class VarselPostgresRepo(
    private val sessionFactory: PostgresSessionFactory,
    private val clock: Clock,
) : VarselRepo {

    /**
     * @param aktiveringsmetadata overlagres bare dersom den ikke er null
     * @param inaktiveringsmetadata overlagres bare dersom den ikke er null
     */
    override fun lagre(
        varsel: Varsel,
        aktiveringsmetadata: String?,
        inaktiveringsmetadata: String?,
        sessionContext: SessionContext?,
    ) {
        sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    insert into varsel (
                        varsel_id,
                        sak_id,
                        type,
                        skal_aktiveres_tidspunkt,
                        skal_aktiveres_eksternt_tidspunkt,
                        skal_aktiveres_begrunnelse,
                        aktiveringstidspunkt,
                        ekstern_aktiveringstidspunkt,
                        skal_inaktiveres_tidspunkt,
                        skal_inaktiveres_begrunnelse,
                        inaktiveringstidspunkt,
                        aktiveringsmetadata,
                        inaktiveringsmetadata,
                        opprettet,
                        sist_endret
                    ) values (
                        :varsel_id,
                        :sak_id,
                        :type,
                        :skal_aktiveres_tidspunkt,
                        :skal_aktiveres_eksternt_tidspunkt,
                        :skal_aktiveres_begrunnelse,
                        :aktiveringstidspunkt,
                         :ekstern_aktiveringstidspunkt,
                        :skal_inaktiveres_tidspunkt,
                        :skal_inaktiveres_begrunnelse,
                        :inaktiveringstidspunkt,
                        :aktiveringsmetadata,
                        :inaktiveringsmetadata,
                        :opprettet,
                        :sist_endret
                    )
                    on conflict (varsel_id) do update set
                        sak_id = :sak_id,
                        type = :type,
                        skal_aktiveres_tidspunkt = :skal_aktiveres_tidspunkt,
                        skal_aktiveres_eksternt_tidspunkt = :skal_aktiveres_eksternt_tidspunkt,
                        skal_aktiveres_begrunnelse = :skal_aktiveres_begrunnelse,
                        aktiveringstidspunkt = :aktiveringstidspunkt,
                        ekstern_aktiveringstidspunkt = :ekstern_aktiveringstidspunkt,
                        skal_inaktiveres_tidspunkt = :skal_inaktiveres_tidspunkt,
                        skal_inaktiveres_begrunnelse = :skal_inaktiveres_begrunnelse,
                        inaktiveringstidspunkt = :inaktiveringstidspunkt,
                        aktiveringsmetadata = coalesce(:aktiveringsmetadata, varsel.aktiveringsmetadata),
                        inaktiveringsmetadata = coalesce(:inaktiveringsmetadata, varsel.inaktiveringsmetadata),
                        sist_endret = :sist_endret
                    """,
                    "varsel_id" to varsel.varselId.toString(),
                    "sak_id" to varsel.sakId.toString(),
                    "type" to when (varsel) {
                        is SkalAktiveres -> "SkalAktiveres"
                        is Aktiv -> "Aktiv"
                        is SkalInaktiveres -> "SkalInaktiveres"
                        is Inaktivert -> "Inaktivert"
                    },
                    "skal_aktiveres_tidspunkt" to varsel.skalAktiveresTidspunkt,
                    "skal_aktiveres_eksternt_tidspunkt" to varsel.skalAktiveresEksterntTidspunkt,
                    "skal_aktiveres_begrunnelse" to varsel.skalAktiveresBegrunnelse,
                    "aktiveringstidspunkt" to varsel.aktiveringstidspunkt,
                    "ekstern_aktiveringstidspunkt" to varsel.eksternAktiveringstidspunkt,
                    "skal_inaktiveres_tidspunkt" to varsel.skalInaktiveresTidspunkt,
                    "skal_inaktiveres_begrunnelse" to varsel.skalInaktiveresBegrunnelse,
                    "inaktiveringstidspunkt" to varsel.inaktiveringstidspunkt,
                    "aktiveringsmetadata" to aktiveringsmetadata,
                    "inaktiveringsmetadata" to inaktiveringsmetadata,
                    "opprettet" to varsel.opprettet,
                    "sist_endret" to varsel.sistEndret,
                ).asUpdate,
            )
        }
    }

    override fun hentVarslerForSakId(sakId: SakId, sessionContext: SessionContext?): Varsler {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select v.*, s.saksnummer, s.fnr from varsel v
                    join sak s on s.id = v.sak_id
                    where v.sak_id = :sak_id
                    order by v.opprettet
                    """,
                    "sak_id" to sakId.toString(),
                ).map { row -> fromRow(row) }.asList,
            ).let { Varsler(it) }
        }
    }

    override fun hentSakerMedVarslerSomSkalAktiveres(
        limit: Int,
        sessionContext: SessionContext?,
    ): List<SakId> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select v.sak_id from varsel v
                    where v.type = 'SkalAktiveres'
                      and v.skal_aktiveres_tidspunkt <= :tidsgrense
                    order by v.opprettet, v.sak_id
                    limit :limit
                    """,
                    "tidsgrense" to nå(clock),
                    "limit" to limit,
                ).map { row -> SakId.fromString(row.string("sak_id")) }.asList,
            )
        }
    }

    override fun hentSakerMedVarslerSomSkalInaktiveres(
        limit: Int,
        sessionContext: SessionContext?,
    ): List<SakId> {
        return sessionFactory.withSession(sessionContext) { session ->
            session.run(
                sqlQuery(
                    """
                    select v.sak_id from varsel v
                    where v.type = 'SkalInaktiveres'
                      and v.skal_inaktiveres_tidspunkt <= :tidsgrense
                    order by v.skal_inaktiveres_tidspunkt, v.sak_id
                    limit :limit
                    """,
                    "tidsgrense" to nå(clock),
                    "limit" to limit,
                ).map { row -> SakId.fromString(row.string("sak_id")) }.asList,
            )
        }
    }

    companion object {
        fun fromRow(row: Row): Varsel {
            val varselId = VarselId(row.string("varsel_id"))
            val sakId = SakId.fromString(row.string("sak_id"))
            val saksnummer = row.string("saksnummer")
            val fnr = Fnr.fromString(row.string("fnr"))
            val skalAktiveresTidspunkt = row.localDateTime("skal_aktiveres_tidspunkt")
            val skalAktiveresEksterntTidspunkt = row.localDateTime("skal_aktiveres_eksternt_tidspunkt")
            val skalAktiveresBegrunnelse = row.string("skal_aktiveres_begrunnelse")
            val aktiveringstidspunkt: LocalDateTime? = row.localDateTimeOrNull("aktiveringstidspunkt")
            val eksternAktiveringstidspunkt: LocalDateTime? = row.localDateTimeOrNull("ekstern_aktiveringstidspunkt")
            val skalInaktiveresTidspunkt: LocalDateTime? = row.localDateTimeOrNull("skal_inaktiveres_tidspunkt")
            val skalInaktiveresBegrunnelse: String? = row.stringOrNull("skal_inaktiveres_begrunnelse")
            val inaktiveringstidspunkt: LocalDateTime? = row.localDateTimeOrNull("inaktiveringstidspunkt")
            val opprettet = row.localDateTime("opprettet")
            val sistEndret = row.localDateTime("sist_endret")
            return when (val type = row.string("type")) {
                "Inaktivert" -> Inaktivert(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    varselId = varselId,
                    skalAktiveresTidspunkt = skalAktiveresTidspunkt,
                    skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
                    skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
                    aktiveringstidspunkt = aktiveringstidspunkt,
                    eksternAktiveringstidspunkt = eksternAktiveringstidspunkt,
                    skalInaktiveresTidspunkt = requireNotNull(skalInaktiveresTidspunkt),
                    skalInaktiveresBegrunnelse = requireNotNull(skalInaktiveresBegrunnelse),
                    inaktiveringstidspunkt = requireNotNull(inaktiveringstidspunkt),
                    opprettet = opprettet,
                    sistEndret = sistEndret,
                )

                "SkalInaktiveres" -> SkalInaktiveres(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    varselId = varselId,
                    skalAktiveresTidspunkt = skalAktiveresTidspunkt,
                    skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
                    skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
                    aktiveringstidspunkt = aktiveringstidspunkt,
                    eksternAktiveringstidspunkt = eksternAktiveringstidspunkt,
                    skalInaktiveresTidspunkt = requireNotNull(skalInaktiveresTidspunkt),
                    skalInaktiveresBegrunnelse = requireNotNull(skalInaktiveresBegrunnelse),
                    opprettet = opprettet,
                    sistEndret = sistEndret,
                )

                "Aktiv" -> Aktiv(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    varselId = varselId,
                    skalAktiveresTidspunkt = skalAktiveresTidspunkt,
                    skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
                    skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
                    aktiveringstidspunkt = requireNotNull(aktiveringstidspunkt),
                    eksternAktiveringstidspunkt = requireNotNull(eksternAktiveringstidspunkt),
                    opprettet = opprettet,
                    sistEndret = sistEndret,
                )

                "SkalAktiveres" -> SkalAktiveres(
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    varselId = varselId,
                    skalAktiveresTidspunkt = skalAktiveresTidspunkt,
                    skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
                    skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
                    opprettet = opprettet,
                    sistEndret = sistEndret,
                )

                else -> throw IllegalStateException("Ukjent varseltype: $type for varselId: $varselId")
            }
        }
    }
}
