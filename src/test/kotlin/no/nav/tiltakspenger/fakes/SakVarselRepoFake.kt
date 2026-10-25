package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.fakes.repos.SakRepoFake
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.infra.db.OptimistiskLåsFeil
import no.nav.tiltakspenger.meldekort.varsler.SakForVarselvurdering
import no.nav.tiltakspenger.meldekort.varsler.SakVarselRepo
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

class SakVarselRepoFake(
    private val sakRepoFake: SakRepoFake,
) : SakVarselRepo {
    private val flaggedForVurdering = Atomic(mutableMapOf<SakId, Instant>())
    private val vurdertTidspunkt = Atomic(mutableMapOf<SakId, LocalDateTime>())

    // Monotonisk stigende tellerbasert tidspunkt for å etterligne clock_timestamp()s egenskap om alltid-nye verdier – slik at den optimistiske låsen kan testes uten å være avhengig av reell wallclock-presisjon.
    private val teller = AtomicLong(0)

    override fun flaggForVarselvurdering(sakId: SakId, sessionContext: SessionContext?) {
        flaggedForVurdering.get()[sakId] = nesteTidspunkt()
    }

    override fun hentSakerSomSkalVurdereVarsel(limit: Int, sessionContext: SessionContext?): List<SakForVarselvurdering> {
        return flaggedForVurdering.get()
            .asSequence()
            // first-flagged-first-served: sorter på tidspunkt. nesteTidspunkt() garanterer unike verdier slik at sorteringen er deterministisk.
            .sortedBy { it.value }
            .mapNotNull { (sakId, tidspunkt) ->
                sakRepoFake.hent(sakId)?.let { sak ->
                    SakForVarselvurdering(
                        sakId = sak.id,
                        saksnummer = sak.saksnummer,
                        fnr = sak.fnr,
                        sistFlaggetTidspunkt = tidspunkt,
                    )
                }
            }
            .take(limit)
            .toList()
    }

    override fun markerVarselVurdert(
        sakId: SakId,
        vurdertTidspunkt: LocalDateTime,
        sistFlaggetTidspunktVedLesing: Instant?,
        sessionContext: SessionContext?,
    ) {
        val nåværende = flaggedForVurdering.get()[sakId]
        if (nåværende != sistFlaggetTidspunktVedLesing) {
            throw OptimistiskLåsFeil(
                "Optimistisk lås slo til for sak $sakId: sist_flagget_tidspunkt er endret siden vi leste saken.",
            )
        }
        flaggedForVurdering.get().remove(sakId)
        this.vurdertTidspunkt.get()[sakId] = vurdertTidspunkt
    }

    private fun nesteTidspunkt(): Instant {
        // Bruk tellerbasert "tidspunkt" for å garantere unike verdier.
        // Vi pakker en AtomicLong inn i et Instant via nanos-feltet, slik at to raske kall alltid får ulike verdier (i motsetning til nå() som kan gi samme verdi).
        val n = teller.incrementAndGet()
        return Instant.EPOCH.plusNanos(n)
    }
}
