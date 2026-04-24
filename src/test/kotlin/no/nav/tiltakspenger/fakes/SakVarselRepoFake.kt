package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.fakes.repos.SakRepoFake
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.varsler.SakForVarselvurdering
import no.nav.tiltakspenger.meldekort.repository.OptimistiskLåsFeil
import no.nav.tiltakspenger.meldekort.repository.SakVarselRepo
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

class SakVarselRepoFake(
    private val sakRepoFake: SakRepoFake,
) : SakVarselRepo {
    private val flaggedForVurdering = Atomic(mutableMapOf<SakId, LocalDateTime>())
    private val vurdertTidspunkt = Atomic(mutableMapOf<SakId, LocalDateTime>())

    // Monotonisk stigende tellerbasert tidspunkt for å etterligne clock_timestamp()s egenskap
    // om alltid-nye verdier – slik at den optimistiske låsen kan testes uten å være avhengig
    // av reell wallclock-presisjon.
    private val teller = AtomicLong(0)

    override fun flaggForVarselvurdering(sakId: SakId, sessionContext: SessionContext?) {
        flaggedForVurdering.get()[sakId] = nesteTidspunkt()
    }

    override fun hentSakerSomSkalVurdereVarsel(limit: Int, sessionContext: SessionContext?): List<SakForVarselvurdering> {
        return flaggedForVurdering.get()
            .mapNotNull { (sakId, tidspunkt) ->
                sakRepoFake.hent(sakId)?.let { sak ->
                    SakForVarselvurdering(sak = sak, sistFlaggetTidspunkt = tidspunkt)
                }
            }
            .take(limit)
    }

    override fun markerVarselVurdert(
        sakId: SakId,
        vurdertTidspunkt: LocalDateTime,
        sistFlaggetTidspunktVedLesing: LocalDateTime?,
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

    private fun nesteTidspunkt(): LocalDateTime {
        // Bruk tellerbasert "tidspunkt" for å garantere unike verdier. Vi pakker en
        // AtomicLong inn i et LocalDateTime via nanos-feltet, slik at to raske kall alltid
        // får ulike verdier (i motsetning til LocalDateTime.now() som kan gi samme verdi).
        val n = teller.incrementAndGet()
        return LocalDateTime.of(1970, 1, 1, 0, 0).plusNanos(n)
    }
}
