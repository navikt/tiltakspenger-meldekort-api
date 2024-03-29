package no.nav.tiltakspenger.meldekort.api.repository

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.tiltakspenger.meldekort.api.domene.UtfallForPeriode
import no.nav.tiltakspenger.meldekort.api.domene.Utfallsperiode
import org.intellij.lang.annotations.Language
import java.util.UUID

class UtfallsperiodeDAO {

    fun hent(grunnlagId: UUID, txSession: TransactionalSession): List<Utfallsperiode> {
        return txSession.run(
            queryOf(hentUtfallsperioderForVedtak, grunnlagId.toString())
                .map { row -> row.toUtfallsperiode() }
                .asList,
        )
    }

    fun lagre(grunnlagId: UUID, utfallsperioder: List<Utfallsperiode>, txSession: TransactionalSession) {
        utfallsperioder.forEach { utfallsperiode ->
            lagreUtfallsperiode(grunnlagId, utfallsperiode, txSession)
        }
    }

    private fun lagreUtfallsperiode(
        grunnlagId: UUID,
        utfallsperiode: Utfallsperiode,
        txSession: TransactionalSession,
    ) {
        txSession.run(
            queryOf(
                lagreUtfallsperiode,
                mapOf(
                    "id" to UUID.randomUUID(),
                    "grunnlagId" to grunnlagId.toString(),
                    "fom" to utfallsperiode.fom,
                    "tom" to utfallsperiode.tom,
                    "antallBarn" to utfallsperiode.antallBarn,
                    "utfall" to utfallsperiode.utfall.name,
                ),
            ).asUpdate,
        )
    }

    private fun Row.toUtfallsperiode(): Utfallsperiode {
        return Utfallsperiode(
            fom = localDate("fom"),
            tom = localDate("tom"),
            antallBarn = int("antall_barn"),
            utfall = UtfallForPeriode.valueOf(string("utfall")),
        )
    }

    @Language("SQL")
    private val lagreUtfallsperiode = """
        insert into utfallsperiode (
            id,
            grunnlag_id,
            fom,
            tom,
            antall_barn,
            utfall
        ) values (
            :id,
            :grunnlagId,
            :fom,
            :tom,
            :antallBarn,
            :utfall
        )
    """.trimIndent()

    @Language("SQL")
    private val hentUtfallsperioderForVedtak = "select * from utfallsperiode where grunnlag_id = ?"
}
