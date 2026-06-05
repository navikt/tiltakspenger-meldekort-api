package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDag
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekort.RegistrertMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortDagTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.MeldekortTilKorrigeringDTO.PreutfyltKorrigeringDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.toDto
import no.nav.tiltakspenger.meldekort.meldekort.registrertTidspunkt
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
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
        val kanSendeInnHelg: Boolean,
    )
}

fun Meldeperiode.tilKorrigeringDTO(
    forrigeRegistrerte: RegistrertMeldekort,
    kanSendeInnHelg: Boolean,
): MeldekortTilKorrigeringDTO {
    val oppdaterteDager = forrigeRegistrerte.dager.map { meldekortDag ->
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
        forrigeMeldekort = forrigeRegistrerte.tilMeldekortTilBrukerDTO(),
        tilUtfylling = PreutfyltKorrigeringDTO(
            meldeperiodeId = id.toString(),
            kjedeId = kjedeId.toString(),
            dager = oppdaterteDager.toDto(),
            periode = periode.toDTO(),
            mottattTidspunktSisteMeldekort = forrigeRegistrerte.registrertTidspunkt(),
            maksAntallDagerForPeriode = maksAntallDagerForPeriode,
            kanSendeInnHelg = kanSendeInnHelg,
        ),
    )
}
