package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.singleOrNullOrThrow
import no.nav.tiltakspenger.meldekort.domene.VarselId
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class Varsler(
    val value: List<Varsel>,
) : List<Varsel> by value {

    val sakId: SakId? = value.map { it.sakId }.distinct().singleOrNullOrThrow()
    val saksnummer: String? = value.map { it.saksnummer }.distinct().singleOrNullOrThrow()
    val fnr: Fnr? = value.map { it.fnr }.distinct().singleOrNullOrThrow()

    val erAlleInaktivertEllerAvbrutt: Boolean by lazy {
        this.all { it.erInaktivertEllerAvbrutt }
    }

    init {
        val aktiveVarsler = value.count { !it.erInaktivertEllerAvbrutt }
        require(aktiveVarsler <= 1) {
            "Varsler: Kun ett varsel kan være aktivt (ikke avbrutt eller inaktivert), men fant $aktiveVarsler"
        }
        require(value.map { it.varselId }.distinct().size == value.map { it.varselId }.size)
        value.zipWithNext { a, b -> require(a.opprettet < b.opprettet) }
    }

    operator fun plus(other: Varsel): Varsler {
        return Varsler(this.value + other)
    }

    fun leggTil(varsel: Varsel.SkalAktiveres): Either<KanIkkeLeggeTilVarsel, Varsler> {
        if (!this.erAlleInaktivertEllerAvbrutt) {
            return KanIkkeLeggeTilVarsel.HarAktivtVarsel.left()
        }
        val varselDato = varsel.skalAktiveresTidspunkt.toLocalDate()
        // Cooldown gjelder kun dersom det faktisk er sendt (aktivert) et varsel samme dag.
        // Et Avbrutt varsel ble aldri sendt, og et Inaktivert varsel kan ha vært aktivert på en annen
        // dato enn skalAktiveresTidspunkt. Vi må derfor sjekke faktisk aktiveringstidspunkt,
        // ellers risikerer vi å droppe hendelser (f.eks. revurdering samme dag som avbrutt varsel).
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
        skalAktiveresBegrunnelse: String,
        clock: Clock,
    ): Either<KanIkkeLeggeTilVarsel, Varsler> {
        val nå = LocalDateTime.now(clock)
        val varsel = Varsel.SkalAktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
            opprettet = nå,
            sistEndret = nå,
        )
        return leggTil(varsel)
    }

    sealed interface KanIkkeLeggeTilVarsel {
        val melding: String

        data object HarAktivtVarsel : KanIkkeLeggeTilVarsel {
            override val melding = "Kan ikke legge til varsel når det finnes aktive varsler"
        }

        data class CooldownIkkeUtløpt(val dato: LocalDate) : KanIkkeLeggeTilVarsel {
            override val melding =
                "Kan ikke legge til varsel med aktiveringsdato $dato - det finnes allerede et varsel for denne datoen"
        }
    }
}
