package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.singleOrNullOrThrow
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel.Inaktivert
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel.SkalAktiveres
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel.SkalInaktiveres
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Alle varsler på en sak. Invariant:
 *  - Maks ett varsel i tilstand [Varsel.SkalAktiveres] eller [Varsel.Aktiv] (et "pågående" varsel).
 *  - Maks ett varsel i tilstand [Varsel.SkalInaktiveres] ("pågående inaktivering").
 *  - Et pågående varsel kan sameksistere med en pågående inaktivering (f.eks. når bruker har
 *    sendt inn meldekortet for en meldeperiode, men det er en ny meldeperiode som mangler
 *    innsending). I den samme transaksjonen forbereder vi inaktivering av det gamle og oppretter
 *    et nytt [Varsel.SkalAktiveres] for den nye meldeperioden.
 */
data class Varsler(
    val value: List<Varsel>,
) : List<Varsel> by value {

    val sakId: SakId? = value.map { it.sakId }.distinct().singleOrNullOrThrow()
    val saksnummer: String? = value.map { it.saksnummer }.distinct().singleOrNullOrThrow()
    val fnr: Fnr? = value.map { it.fnr }.distinct().singleOrNullOrThrow()

    val pågåendeAktivering: SkalAktiveres? = value.filterIsInstance<SkalAktiveres>().singleOrNullOrThrow()
    val pågåendeInaktivering: SkalInaktiveres? = value.filterIsInstance<SkalInaktiveres>().singleOrNullOrThrow()

    /** Pågående varsel for ny meldeperiode – enten [Varsel.SkalAktiveres] eller [Varsel.Aktiv]. */
    val pågåendeOppretting: Varsel? = value.singleOrNullOrThrow { it is SkalAktiveres || it is Varsel.Aktiv }

    /** True dersom det ikke finnes noe pågående varsel (hverken oppretting eller inaktivering). */
    val erAlleInaktivert: Boolean by lazy { value.all { it.erInaktivert } }

    init {
        val oppretting = value.count { it is SkalAktiveres || it is Varsel.Aktiv }
        require(oppretting <= 1) {
            "Varsler: Maks ett varsel kan være i SkalAktiveres/Aktiv, men fant $oppretting"
        }
        val inaktivering = value.count { it is SkalInaktiveres }
        require(inaktivering <= 1) {
            "Varsler: Maks ett varsel kan være i SkalInaktiveres, men fant $inaktivering"
        }
        require(value.distinctBy { it.varselId }.size == value.size) {
            "Varsler: Flere varsler med samme varselId"
        }
        value.zipWithNext { a, b ->
            require(a.opprettet < b.opprettet) {
                "Varsler: opprettet må være strengt stigende. Fant ${a.varselId}=${a.opprettet} og ${b.varselId}=${b.opprettet}"
            }
        }
    }

    operator fun plus(other: Varsel): Varsler {
        return Varsler(this.value + other)
    }

    fun oppdater(other: Varsel): Varsler {
        require(value.any { it.varselId == other.varselId }) {
            "Varsler: Kan ikke oppdatere ukjent varselId ${other.varselId}"
        }

        return Varsler(
            value.map { if (it.varselId == other.varselId) other else it },
        )
    }

    fun aktiver(
        varselId: VarselId,
        aktiveringstidspunkt: LocalDateTime,
    ): Pair<Varsler, Varsel.Aktiv> {
        val varsel = hent(varselId)
        require(varsel is SkalAktiveres) {
            "Varsler: Kan ikke aktivere varsel ${varsel.varselId} av type ${varsel::class.simpleName}"
        }
        val aktivertVarsel = varsel.aktiver(aktiveringstidspunkt)
        return oppdater(aktivertVarsel) to aktivertVarsel
    }

    fun forberedInaktivering(
        varselId: VarselId,
        skalInaktiveresTidspunkt: LocalDateTime,
        skalInaktiveresBegrunnelse: String,
    ): Pair<Varsler, SkalInaktiveres> {
        val oppdatert = when (val varsel = hent(varselId)) {
            is Varsel.Aktiv -> varsel.forberedInaktivering(
                skalInaktiveresTidspunkt = skalInaktiveresTidspunkt,
                skalInaktiveresBegrunnelse = skalInaktiveresBegrunnelse,
            )

            is SkalAktiveres -> varsel.forberedInaktivering(
                skalInaktiveresTidspunkt = skalInaktiveresTidspunkt,
                skalInaktiveresBegrunnelse = skalInaktiveresBegrunnelse,
            )

            is SkalInaktiveres, is Inaktivert -> error(
                "Varsler: Kan ikke forberede inaktivering for varsel ${varsel.varselId} av type ${varsel::class.simpleName}",
            )
        }
        return oppdater(oppdatert) to oppdatert
    }

    fun inaktiver(
        varselId: VarselId,
        inaktiveringstidspunkt: LocalDateTime,
    ): Pair<Varsler, Inaktivert> {
        val varsel = hent(varselId)
        require(varsel is SkalInaktiveres) {
            "Varsler: Kan ikke inaktivere varsel ${varsel.varselId} av type ${varsel::class.simpleName}"
        }
        val inaktivertVarsel = varsel.inaktiver(inaktiveringstidspunkt)
        return oppdater(inaktivertVarsel) to inaktivertVarsel
    }

    fun leggTil(varsel: SkalAktiveres): Either<KanIkkeLeggeTilVarsel, Varsler> {
        if (pågåendeOppretting != null) {
            return KanIkkeLeggeTilVarsel.HarPågåendeOppretting.left()
        }
        val varselDato = varsel.skalAktiveresTidspunkt.toLocalDate()
        // Cooldown gjelder kun dersom det faktisk er sendt (aktivert) et varsel samme dag.
        // Et Inaktivert varsel kan ha vært aktivert på en annen dato enn skalAktiveresTidspunkt.
        // Vi må derfor sjekke faktisk aktiveringstidspunkt, ellers risikerer vi å droppe hendelser.
        if (harAktivertVarselSammeDag(varselDato)) {
            return KanIkkeLeggeTilVarsel.CooldownIkkeUtløpt(varselDato).left()
        }
        return (this + varsel).right()
    }

    fun leggTil(
        varselId: VarselId = VarselId.random(),
        sakId: SakId,
        saksnummer: String,
        fnr: Fnr,
        skalAktiveresTidspunkt: LocalDateTime,
        skalAktiveresEksterntTidspunkt: LocalDateTime,
        skalAktiveresBegrunnelse: String,
        clock: Clock,
    ): Either<KanIkkeLeggeTilVarsel, Varsler> {
        val nå = nå(clock)
        val varsel = SkalAktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
            skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
            opprettet = nå,
            sistEndret = nå,
        )
        return leggTil(varsel)
    }

    sealed interface KanIkkeLeggeTilVarsel {
        val melding: String

        data object HarPågåendeOppretting : KanIkkeLeggeTilVarsel {
            override val melding = "Kan ikke legge til varsel når det finnes et pågående varsel (SkalAktiveres/Aktiv)"
        }

        data class CooldownIkkeUtløpt(val dato: LocalDate) : KanIkkeLeggeTilVarsel {
            override val melding =
                "Kan ikke legge til varsel med aktiveringsdato $dato - det finnes allerede et varsel for denne datoen"
        }
    }

    private fun hent(varselId: VarselId): Varsel {
        return requireNotNull(value.singleOrNull { it.varselId == varselId }) {
            "Varsler: Fant ikke varselId $varselId"
        }
    }
}
