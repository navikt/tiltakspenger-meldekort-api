package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.toDto
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.MeldekortTilKorrigeringDTO.PreutfyltKorrigeringDTO
import java.time.Clock
import java.time.LocalDateTime

data class MeldekortTilKorrigeringDTO(
    val forrigeMeldekort: MeldekortTilBrukerDTO,
    val tilUtfylling: PreutfyltKorrigeringDTO,
) {

    data class PreutfyltKorrigeringDTO(
        val meldeperiodeId: String,
        val kjedeId: String,
        val dager: List<MeldekortDagTilBrukerDTO>,
        val periode: PeriodeDTO,
        val mottattTidspunktSisteMeldekort: LocalDateTime,
        val maksAntallDagerForPeriode: Int,
    )
}

fun Meldeperiode.tilKorrigeringDTO(forrigeMeldekort: Meldekort, clock: Clock): MeldekortTilKorrigeringDTO {
    requireNotNull(forrigeMeldekort.mottatt)

    val oppdaterteDager = forrigeMeldekort.dager.map { meldekortDag ->
        MeldekortDag(
            dag = meldekortDag.dag,
            status = if (girRett[meldekortDag.dag]!!) {
                if (meldekortDag.status == MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER) {
                    MeldekortDagStatus.IKKE_BESVART
                } else {
                    meldekortDag.status
                }
            } else {
                MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
            },
        )
    }

    return MeldekortTilKorrigeringDTO(
        forrigeMeldekort = forrigeMeldekort.tilMeldekortTilBrukerDTO(clock),
        tilUtfylling = PreutfyltKorrigeringDTO(
            meldeperiodeId = id.toString(),
            kjedeId = kjedeId.toString(),
            dager = oppdaterteDager.toDto(),
            periode = periode.toDTO(),
            mottattTidspunktSisteMeldekort = forrigeMeldekort.mottatt,
            maksAntallDagerForPeriode = maksAntallDagerForPeriode,
        ),
    )
}
