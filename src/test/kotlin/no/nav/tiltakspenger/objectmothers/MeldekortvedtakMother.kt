package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldeperiodebehandling
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldeperiodebehandlingDag
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon
import java.time.LocalDateTime

interface MeldekortvedtakMother {

    /**
     * Bygger et [Meldekortvedtak] (f.eks. papirmeldekort behandlet i saksbehandling-api) for kjeden
     * til det gitte [meldekort]et. Praktisk for å teste at uinnsendte meldekort ekskluderes når det
     * finnes et meldekortvedtak for kjeden.
     */
    fun meldekortvedtak(
        meldekort: BrukersMeldekort,
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(fixedClock),
        erKorrigering: Boolean = false,
        erAutomatiskBehandlet: Boolean = false,
    ): Meldekortvedtak = meldekortvedtak(
        sakId = meldekort.sakId,
        meldeperiodeId = meldekort.meldeperiode.id,
        meldeperiodeKjedeId = meldekort.meldeperiode.kjedeId,
        periode = meldekort.periode,
        id = id,
        opprettet = opprettet,
        erKorrigering = erKorrigering,
        erAutomatiskBehandlet = erAutomatiskBehandlet,
    )

    fun meldekortvedtak(
        sakId: SakId,
        meldeperiodeId: MeldeperiodeId,
        meldeperiodeKjedeId: MeldeperiodeKjedeId,
        periode: Periode,
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(fixedClock),
        erKorrigering: Boolean = false,
        erAutomatiskBehandlet: Boolean = false,
        dager: List<MeldeperiodebehandlingDag> = periode.tilDager().map { dato ->
            MeldeperiodebehandlingDag(
                dato = dato,
                status = MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                reduksjon = Reduksjon.INGEN_REDUKSJON,
                beløp = 285,
                beløpBarnetillegg = 0,
            )
        },
    ): Meldekortvedtak = Meldekortvedtak(
        id = id,
        sakId = sakId,
        opprettet = opprettet,
        erKorrigering = erKorrigering,
        erAutomatiskBehandlet = erAutomatiskBehandlet,
        meldeperiodebehandlinger = listOf(
            Meldeperiodebehandling(
                meldeperiodeId = meldeperiodeId,
                meldeperiodeKjedeId = meldeperiodeKjedeId,
                brukersMeldekortId = null,
                periode = periode,
                dager = dager,
            ),
        ),
    )
}
