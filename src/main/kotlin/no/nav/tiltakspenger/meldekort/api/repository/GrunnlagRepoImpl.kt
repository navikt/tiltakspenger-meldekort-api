package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.tiltakspenger.meldekort.api.db.DataSource
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.Personopplysninger
import no.nav.tiltakspenger.meldekort.api.domene.Status
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import org.intellij.lang.annotations.Language
import java.util.*

class GrunnlagRepoImpl(
    private val tiltakRepo: GrunnlagTiltakRepo,
) : GrunnlagRepo {
    override fun lagre(dto: MeldekortGrunnlag) {
        sessionOf(DataSource.hikariDataSource).use {
            it.transaction {
                it.run(
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
                )
            }.also {
                dto.tiltak.forEach { tiltak ->
                    tiltakRepo.lagre(dto.id.toString(), tiltak)
                }
            }
        }
    }

    override fun hentAktiveGrunnlagForInneværendePeriode(): List<MeldekortGrunnlag> {
        return sessionOf(DataSource.hikariDataSource).use {
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
    }

    private fun Row.toGrunnlag(txSession: TransactionalSession): MeldekortGrunnlag {
        return MeldekortGrunnlag(
            id = UUID.fromString(string("id")),
            sakId = string("sak_id"),
            behandlingId = string("behandling_id"),
            vedtakId = string("vedtak_id"),
            status = Status.valueOf(string("status")),
            vurderingsperiode = Periode(
                fra = localDate("fom"),
                til = localDate("tom"),
            ),
            tiltak = tiltakRepo.hentTiltakForGrunnlag(string("id"), txSession),
            personopplysninger = Personopplysninger(
                fornavn = string("fornavn"),
                etternavn = string("etternavn"),
                ident = string("ident"),
            ),
        )
    }

    override fun hentGrunnlag(id: UUID): MeldekortGrunnlag? {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction {
                it.run(
                    queryOf(
                        sqlHentGrunnlag,
                        mapOf(
                            "id" to id.toString(),
                        ),
                    ).map { row ->
                        row.toGrunnlag(it)
                    }.asSingle,
                )
            }
        }
    }

    override fun hentForBehandling(behandlingId: String): MeldekortGrunnlag? {
        return sessionOf(DataSource.hikariDataSource).use {
            it.transaction {
                it.run(
                    queryOf(
                        sqlHentForBehandling,
                        mapOf(
                            "behandling_id" to behandlingId,
                        ),
                    ).map { row ->
                        row.toGrunnlag(it)
                    }.asSingle,
                )
            }
        }
    }

    @Language("SQL")
    private val sqlhentAktiveInneværendePeriodeGrunnlag = """
        select * from grunnlag
        where status = 'AKTIV'
         and fom <= now()
         and tom >= now()
    """.trimIndent()

    @Language("SQL")
    private val sqlHentForBehandling = """
        select * from grunnlag
        where behandling_id = :behandling_id
    """.trimIndent()

    @Language("SQL")
    private val sqlHentGrunnlag = """
        select * from grunnlag
        where id = :id
    """.trimIndent()

    @Language("SQL")
    private val sqlLagreGrunnlag = """
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
