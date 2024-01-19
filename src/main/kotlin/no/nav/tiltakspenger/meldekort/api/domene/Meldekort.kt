package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

sealed interface Meldekort {
    val id: UUID
    val løpenr: Int
    val fom: LocalDate
    val tom: LocalDate
    val forrigeMeldekort: Meldekort?
    val meldekortDager: List<MeldekortDag>
    val sistEndret: LocalDateTime
    val opprettet: LocalDateTime

    fun erÅpen() = false

    fun leggTilForrigeMeldekort(meldekort: Meldekort): Meldekort

    data class Åpent(
        override val id: UUID,
        override val løpenr: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
        override val forrigeMeldekort: Meldekort? = null,
        override val meldekortDager: List<MeldekortDag>,
        override val sistEndret: LocalDateTime = LocalDateTime.now(),
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : Meldekort {
        override fun erÅpen() = true

        override fun leggTilForrigeMeldekort(meldekort: Meldekort) =
            this.copy(forrigeMeldekort = meldekort)
    }

    data class Innsendt(
        override val id: UUID,
        override val løpenr: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
        override val forrigeMeldekort: Meldekort?,
        override val meldekortDager: List<MeldekortDag>,
        override val sistEndret: LocalDateTime,
        override val opprettet: LocalDateTime,
        val sendtInn: LocalDateTime,
        val saksbehandler: String,
    ) : Meldekort {
        override fun erÅpen() = false

        override fun leggTilForrigeMeldekort(meldekort: Meldekort) =
            this.copy(forrigeMeldekort = meldekort)
    }
}

fun Meldekort.valider(): Boolean = when (this) {
    is Meldekort.Åpent -> {
        this.validerDager()
        this.validerLøpenummer()
        this.validerForrigemeldekort()
        true
    }
    is Meldekort.Innsendt -> throw IllegalStateException("Kan ikke validere et innsendt meldekort")
}

private fun Meldekort.validerLøpenummer() {
    check(løpenr > 0) { "Løpenummer må være større enn 0" }
}

private fun Meldekort.validerForrigemeldekort() {
    if (løpenr == 1) {
        check(forrigeMeldekort == null) { "Meldekortet 1 har ikke lov til å ha et forrige meldekort" }
    } else {
        checkNotNull(forrigeMeldekort) { "Meldekortet med løpenr $løpenr mangler et forrige meldekort" }
        check(forrigeMeldekort is Meldekort.Innsendt) { "Forrige meldekort er ikke innsendt" }
    }
}

private fun Meldekort.validerDager() =
    check(meldekortDager.none { it.status == MeldekortDagStatus.IKKE_UTFYLT }) {
        "Meldekortet har dager som ikke er utfylt"
    }

fun Meldekort.Åpent.godkjennMeldekort(saksbehandler: String) =
    Meldekort.Innsendt(
        id = id,
        løpenr = løpenr,
        fom = fom,
        tom = tom,
        forrigeMeldekort = forrigeMeldekort,
        meldekortDager = meldekortDager,
        sendtInn = LocalDateTime.now(),
        saksbehandler = saksbehandler,
        sistEndret = sistEndret,
        opprettet = opprettet,
    )
