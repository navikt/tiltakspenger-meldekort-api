package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

private val EN_TIME: Duration = Duration.ofHours(1)
private val TI_MINUTTER: Duration = Duration.ofMinutes(10)

internal sealed interface BeholderPågåendeVarselÅrsak : VurderVarselUtfall {
    data object PlanlagtAktiveringErNærNokEllerLikSkalAktiveres : BeholderPågåendeVarselÅrsak

    data object PlanlagtAktiveringErIkkeTidligNokTilÅErstatteSkalAktiveres : BeholderPågåendeVarselÅrsak

    data object PlanlagtAktiveringErInnenforEnTimeAktivtVarsel : BeholderPågåendeVarselÅrsak
}

internal data class PlanlagtAktivering(
    val skalAktiveresTidspunkt: LocalDateTime,
    val skalAktiveresEksterntTidspunkt: LocalDateTime,
    val begrunnelse: String,
) {
    fun vurderPågåendeVarsel(
        clock: Clock,
        pågående: Varsel,
    ): Either<BeholderPågåendeVarselÅrsak, Unit> {
        return when (pågående) {
            is Varsel.SkalAktiveres -> vurderSkalAktiveres(clock, pågående.skalAktiveresTidspunkt)

            is Varsel.Aktiv -> vurderAktivtVarsel(clock)

            is Varsel.SkalInaktiveres, is Varsel.Inaktivert -> {
                error("PlanlagtAktivering.vurderPågåendeVarsel: forventet SkalAktiveres eller Aktiv, fikk ${pågående::class.simpleName}")
            }
        }
    }

    fun vurderSkalAktiveres(
        clock: Clock,
        pågåendeSkalAktiveresTidspunkt: LocalDateTime,
    ): Either<BeholderPågåendeVarselÅrsak, Unit> {
        val nå = nå(clock)
        val planlagtEffektivTid = maxOf(skalAktiveresTidspunkt, nå)
        val pågåendeEffektivTid = maxOf(pågåendeSkalAktiveresTidspunkt, nå)
        val differanse = Duration.between(planlagtEffektivTid, pågåendeEffektivTid).abs()

        if (differanse <= EN_TIME && planlagtEffektivTid >= pågåendeEffektivTid) {
            return BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErNærNokEllerLikSkalAktiveres.left()
        }

        if (
            planlagtEffektivTid < pågåendeEffektivTid &&
            (differanse <= TI_MINUTTER || pågåendeEffektivTid <= nå.plusMinutes(10))
        ) {
            return BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErIkkeTidligNokTilÅErstatteSkalAktiveres.left()
        }

        return Unit.right()
    }

    fun vurderAktivtVarsel(clock: Clock): Either<BeholderPågåendeVarselÅrsak, Unit> {
        return if (skalAktiveresTidspunkt <= nå(clock).plusHours(1)) {
            BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErInnenforEnTimeAktivtVarsel.left()
        } else {
            Unit.right()
        }
    }
}
