package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate
import java.util.*

sealed interface Meldekort {
    val id: UUID
    val fom: LocalDate
    val tom: LocalDate
    val forrigeMeldekort: Meldekort?
    val meldekortDager: List<MeldekortDag>
    val sistEndret: LocalDate
    val opprettet: LocalDate

    fun erÅpen() = false
//    fun valider(): Boolean

    fun leggTilForrigeMeldekort(meldekort: Meldekort): Meldekort

    data class Åpent(
        override val id: UUID,
        override val fom: LocalDate,
        override val tom: LocalDate,
        override val forrigeMeldekort: Meldekort? = null,
        override val meldekortDager: List<MeldekortDag>,
        override val sistEndret: LocalDate = LocalDate.now(),
        override val opprettet: LocalDate = LocalDate.now(),
    ) : Meldekort {
        override fun erÅpen() = true

        override fun leggTilForrigeMeldekort(meldekort: Meldekort) =
            this.copy(forrigeMeldekort = meldekort)

//        override fun valider(): Boolean {
//            return meldekortDager.any { it.status == MeldekortDagStatus.IKKE_UTFYLT}
//        }

//        fun godkjennMeldekort(beslutter: String) =
//            Innsendt(
//                id = id,
//                fom = fom,
//                tom = tom,
//                forrigeMeldekort = forrigeMeldekort,
//                meldekortDager = meldekortDager,
//                sendtInn = LocalDate.now(),
//                beslutter = beslutter,
//            )
    }
    data class Innsendt(
        override val id: UUID,
        override val fom: LocalDate,
        override val tom: LocalDate,
        override val forrigeMeldekort: Meldekort?,
        override val meldekortDager: List<MeldekortDag>,
        override val sistEndret: LocalDate,
        override val opprettet: LocalDate,
        val sendtInn: LocalDate,
        val beslutter: String,
    ) : Meldekort {
        override fun erÅpen() = false

        override fun leggTilForrigeMeldekort(meldekort: Meldekort) =
            this.copy(forrigeMeldekort = meldekort)

//        override fun valider(): Boolean {
//            throw IllegalStateException("Kan ikke validere et innsendt meldekort")
//        }
    }
}

 fun Meldekort.valider(): Boolean = when (this) {
    is Meldekort.Åpent -> {
        this.validerDager()
        this.validerForrigemeldekort()
        true
    }
    is Meldekort.Innsendt -> throw IllegalStateException("Kan ikke validere et innsendt meldekort")
 }

private fun Meldekort.validerForrigemeldekort() =
    check(forrigeMeldekort == null) {
        "Meldekortet har et forrige meldekort"
    }

private fun Meldekort.validerDager() =
    check(meldekortDager.none { it.status == MeldekortDagStatus.IKKE_UTFYLT }) {
        "Meldekortet har dager som ikke er utfylt"
    }

fun Meldekort.Åpent.godkjennMeldekort(beslutter: String) =
    Meldekort.Innsendt(
        id = id,
        fom = fom,
        tom = tom,
        forrigeMeldekort = forrigeMeldekort,
        meldekortDager = meldekortDager,
        sendtInn = LocalDate.now(),
        beslutter = beslutter,
        sistEndret = sistEndret,
        opprettet = opprettet,
    )
