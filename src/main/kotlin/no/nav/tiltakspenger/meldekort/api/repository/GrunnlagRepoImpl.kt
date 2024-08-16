package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.Personopplysninger
import no.nav.tiltakspenger.meldekort.api.domene.Status
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import org.intellij.lang.annotations.Language
import java.util.UUID

class GrunnlagRepoImpl(
    private val tiltakRepo: GrunnlagTiltakRepo,
    private val utfallsperiodeDAO: UtfallsperiodeDAO,
    private val sessionFactory: PostgresSessionFactory,
) : GrunnlagRepo {
    override fun lagre(
        dto: MeldekortGrunnlag,
        transactionContext: TransactionContext?,
    ) {
        sessionFactory.withTransaction(transactionContext) { tx ->
            tx
                .run(
                    queryOf(
                        sqlLagreGrunnlag,
                        mapOf(
                            "id" to dto.id.toString(),
                            "sakId" to dto.sakId,
                            "behandlingId" to dto.behandlingId,
                            "vedtakId" to dto.vedtakId,
                            "status" to dto.status.name,
                            "fom" to dto.vurderingsperiode.fra,
                            "tom" to dto.vurderingsperiode.til,
                            "fornavn" to dto.personopplysninger.fornavn,
                            "etternavn" to dto.personopplysninger.etternavn,
                            "ident" to dto.personopplysninger.ident,
                        ),
                    ).asUpdate,
                ).also {
                    dto.tiltak.forEach { tiltak ->
                        tiltakRepo.lagre(dto.id.toString(), tiltak, tx)
                    }
                    utfallsperiodeDAO.lagre(dto.id, dto.utfallsperioder, tx)
                }
        }
    }

    override fun hentAktiveGrunnlagForInneværendePeriode(): List<MeldekortGrunnlag> =
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction { txSession ->
                txSession.run(
                    queryOf(
                        sqlhentAktiveInneværendePeriodeGrunnlag,
                    ).map { row ->
                        row.toGrunnlag(txSession)
                    }.asList,
                )
            }
        }

    private fun Row.toGrunnlag(session: Session): MeldekortGrunnlag {
        val grunnlagId = UUID.fromString(string("id"))
        return MeldekortGrunnlag(
            id = grunnlagId,
            sakId = string("sak_id"),
            behandlingId = string("behandling_id"),
            vedtakId = string("vedtak_id"),
            status = Status.valueOf(string("status")),
            vurderingsperiode =
            Periode(
                fra = localDate("fom"),
                til = localDate("tom"),
            ),
            tiltak = tiltakRepo.hentTiltakForGrunnlag(string("id"), session),
            personopplysninger =
            Personopplysninger(
                fornavn = string("fornavn"),
                etternavn = string("etternavn"),
                ident = string("ident"),
            ),
            utfallsperioder = utfallsperiodeDAO.hent(grunnlagId, session),
        )
    }

    override fun hentGrunnlag(id: UUID): MeldekortGrunnlag? =
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        sqlHentGrunnlag,
                        mapOf(
                            "id" to id.toString(),
                        ),
                    ).map { row ->
                        row.toGrunnlag(tx)
                    }.asSingle,
                )
            }
        }

    override fun hentGrunnlagForVedtakId(vedtakId: UUID): MeldekortGrunnlag? =
        sessionOf(DataSource.hikariDataSource).use {
            it.run(
                queryOf(
                    """select * from grunnlag where vedtakId = :vedtakId""",
                    mapOf(
                        "vedtakId" to vedtakId.toString(),
                    ),
                ).map { row ->
                    row.toGrunnlag(it)
                }.asSingle,
            )
        }

    override fun hentForBehandling(behandlingId: String): MeldekortGrunnlag? =
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction { tx ->
                tx.run(
                    queryOf(
                        sqlHentForBehandling,
                        mapOf(
                            "behandling_id" to behandlingId,
                        ),
                    ).map { row ->
                        row.toGrunnlag(tx)
                    }.asSingle,
                )
            }
        }

    @Language("SQL")
    private val sqlhentAktiveInneværendePeriodeGrunnlag =
        """
        select * from grunnlag
        where status = 'AKTIV'
         and fom <= now()
         and tom >= now()
        """.trimIndent()

    @Language("SQL")
    private val sqlHentForBehandling =
        """
        select * from grunnlag
        where behandling_id = :behandling_id
        """.trimIndent()

    @Language("SQL")
    private val sqlHentGrunnlag =
        """
        select * from grunnlag
        where id = :id
        """.trimIndent()

    @Language("SQL")
    private val sqlLagreGrunnlag =
        """
        insert into grunnlag (
            id,
            sak_id,
            behandling_id,
            vedtak_id,
            status,
            fom,
            tom,
            fornavn,
            etternavn,
            ident
        ) values (
            :id,
            :sakId,
            :behandlingId,
            :vedtakId,
            :status,
            :fom,
            :tom,
            :fornavn,
            :etternavn,
            :ident
        )
        """.trimIndent()
}
