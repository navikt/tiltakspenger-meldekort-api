package no.nav.tiltakspenger.meldekort.mottak.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldeperiodebehandling
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldeperiodebehandlingDag
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode.Companion.kanFyllesUtFraOgMed
import no.nav.tiltakspenger.meldekort.mottak.MottattSak

/**
 * Mapping fra DTO mottatt fra saksbehandling-api til skrivemodellen [MottattSak].
 *
 * Bor i infra-laget for å unngå at domenet kjenner til ekstern DTO/transportstruktur.
 */
fun SakTilMeldekortApiDTO.tilMottattSak(): MottattSak {
    val sakId = SakId.fromString(this.sakId)
    val fnr = Fnr.fromString(this.fnr)

    val meldeperioder = this.meldeperioder.map {
        val periode = it.periodeDTO.toDomain()
        Meldeperiode(
            id = MeldeperiodeId.fromString(it.id),
            kjedeId = MeldeperiodeKjedeId(it.kjedeId),
            versjon = it.versjon,
            sakId = sakId,
            saksnummer = this.saksnummer,
            fnr = fnr,
            periode = periode,
            opprettet = it.opprettet,
            maksAntallDagerForPeriode = it.antallDagerForPeriode,
            girRett = it.girRett,
            kanFyllesUtFraOgMed = periode.kanFyllesUtFraOgMed(),
        )
    }.sortedBy { it.periode.fraOgMed }

    val meldekortvedtak = this.meldekortvedtak.map { vedtakDTO ->
        Meldekortvedtak(
            id = VedtakId.fromString(vedtakDTO.id),
            sakId = sakId,
            opprettet = vedtakDTO.opprettet,
            erKorrigering = vedtakDTO.erKorrigering,
            erAutomatiskBehandlet = vedtakDTO.erAutomatiskBehandlet,
            meldeperiodebehandlinger = vedtakDTO.meldeperiodebehandlinger.map { behandlingDTO ->
                Meldeperiodebehandling(
                    meldeperiodeId = MeldeperiodeId.fromString(behandlingDTO.meldeperiodeId),
                    meldeperiodeKjedeId = MeldeperiodeKjedeId(behandlingDTO.meldeperiodeKjedeId),
                    brukersMeldekortId = behandlingDTO.brukersMeldekortId?.let { MeldekortId.fromString(it) },
                    periode = behandlingDTO.periodeDTO.toDomain(),
                    dager = behandlingDTO.dager.map { dagDTO ->
                        MeldeperiodebehandlingDag(
                            dato = dagDTO.dato,
                            status = dagDTO.status.toDomain(),
                            reduksjon = dagDTO.reduksjon.toDomain(),
                            beløp = dagDTO.beløp,
                            beløpBarnetillegg = dagDTO.beløpBarnetillegg,
                        )
                    },
                )
            },
        )
    }

    return MottattSak(
        id = sakId,
        fnr = fnr,
        saksnummer = this.saksnummer,
        meldeperioder = meldeperioder,
        harSoknadUnderBehandling = harSoknadUnderBehandling,
        kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        meldekortvedtak = meldekortvedtak,
    )
}

private fun SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.toDomain(): MeldekortDagStatus = when (this) {
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU -> MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.FRAVÆR_ANNET -> MeldekortDagStatus.FRAVÆR_ANNET
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.IKKE_BESVART -> MeldekortDagStatus.IKKE_BESVART
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.IKKE_TILTAKSDAG -> MeldekortDagStatus.IKKE_TILTAKSDAG
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
}

private fun SakTilMeldekortApiDTO.MeldekortvedtakDTO.Reduksjon.toDomain(): Reduksjon = when (this) {
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Reduksjon.INGEN_REDUKSJON -> Reduksjon.INGEN_REDUKSJON
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Reduksjon.REDUKSJON -> Reduksjon.REDUKSJON
    SakTilMeldekortApiDTO.MeldekortvedtakDTO.Reduksjon.YTELSEN_FALLER_BORT -> Reduksjon.YTELSEN_FALLER_BORT
}
