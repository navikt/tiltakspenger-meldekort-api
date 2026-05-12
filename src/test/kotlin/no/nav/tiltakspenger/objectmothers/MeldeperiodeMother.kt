package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode.Companion.kanFyllesUtFraOgMed
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

private val sakIdPerFnr = ConcurrentHashMap<Fnr, SakId>()

interface MeldeperiodeMother {
    fun meldeperiode(
        id: MeldeperiodeId = MeldeperiodeId.random(),
        periode: Periode = ObjectMother.periode(),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        fnr: Fnr = Fnr.random(),
        sakId: SakId = sakIdForFnr(fnr),
        saksnummer: String = saksnummerForSakId(sakId),
        versjon: Int = 1,
        opprettet: LocalDateTime,
        girRett: Map<LocalDate, Boolean> = periode.tilGirRett(),
        antallDagerForPeriode: Int = girRett.filter { it.value }.size,
        kanFyllesUtFraOgMed: LocalDateTime = periode.kanFyllesUtFraOgMed(),
    ): Meldeperiode {
        require(MeldeperiodeKjedeId.fraPeriode(periode) == kjedeId) {
            "KjedeId må være lik MeldeperiodeKjedeId.fraPeriode(periode)"
        }
        return Meldeperiode(
            id = id,
            periode = periode,
            saksnummer = saksnummer,
            sakId = sakId,
            fnr = fnr,
            kjedeId = kjedeId,
            versjon = versjon,
            opprettet = opprettet,
            maksAntallDagerForPeriode = antallDagerForPeriode,
            girRett = girRett,
            kanFyllesUtFraOgMed = kanFyllesUtFraOgMed,
        )
    }

    fun meldeperiodeDto(
        id: String = MeldeperiodeId.random().toString(),
        periode: Periode = ObjectMother.periode(),
        versjon: Int = 1,
        opprettet: LocalDateTime,
        girRett: Map<LocalDate, Boolean> = periode.tilGirRett(),
        antallDagerForPeriode: Int = min(girRett.filter { it.value }.size, 10),
    ): SakTilMeldekortApiDTO.MeldeperiodeDTO {
        return SakTilMeldekortApiDTO.MeldeperiodeDTO(
            id = id,
            kjedeId = MeldeperiodeKjedeId.fraPeriode(periode).toString(),
            versjon = versjon,
            opprettet = opprettet,
            girRett = girRett,
            periodeDTO = periode.toDTO(),
            antallDagerForPeriode = antallDagerForPeriode,
        )
    }

    private fun Periode.tilGirRett(): Map<LocalDate, Boolean> = tilDager()
        .associateWith { value -> listOf(value.dayOfWeek).none { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } }
}

fun sakIdForFnr(fnr: Fnr): SakId {
    return sakIdPerFnr.computeIfAbsent(fnr) { SakId.random() }
}

fun saksnummerForSakId(sakId: SakId): String {
    return "SAK-$sakId"
}
