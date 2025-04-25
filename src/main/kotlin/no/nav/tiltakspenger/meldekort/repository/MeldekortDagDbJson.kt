package no.nav.tiltakspenger.meldekort.repository

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.domene.IMeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import java.time.LocalDate

private data class MeldekortDagDbJson(
    val dag: LocalDate,
    val status: MeldekortDagDbStatus,
) {
    @Suppress("EnumEntryName")
    enum class MeldekortDagDbStatus {
        DELTATT,
        FRAVÆR_SYK,
        FRAVÆR_SYKT_BARN,
        FRAVÆR_ANNET,
        IKKE_DELTATT,
        IKKE_REGISTRERT,
    }

    fun toDomain(): MeldekortDag {
        return MeldekortDag(
            dag = dag,
            status = when (status) {
                MeldekortDagDbStatus.DELTATT -> MeldekortDagStatus.DELTATT
                MeldekortDagDbStatus.FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
                MeldekortDagDbStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
                MeldekortDagDbStatus.FRAVÆR_ANNET -> MeldekortDagStatus.FRAVÆR_ANNET
                MeldekortDagDbStatus.IKKE_DELTATT -> MeldekortDagStatus.IKKE_DELTATT
                MeldekortDagDbStatus.IKKE_REGISTRERT -> MeldekortDagStatus.IKKE_REGISTRERT
            },
        )
    }
}

fun List<IMeldekortDag>.toDbJson(): String {
    return this.map { dag ->
        MeldekortDagDbJson(
            dag = dag.dag,
            status = when (dag.status) {
                MeldekortDagStatus.DELTATT -> MeldekortDagDbJson.MeldekortDagDbStatus.DELTATT
                MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_SYK
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_SYKT_BARN
                MeldekortDagStatus.FRAVÆR_ANNET -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_ANNET
                MeldekortDagStatus.IKKE_DELTATT -> MeldekortDagDbJson.MeldekortDagDbStatus.IKKE_DELTATT
                MeldekortDagStatus.IKKE_REGISTRERT -> MeldekortDagDbJson.MeldekortDagDbStatus.IKKE_REGISTRERT
            },
        )
    }.let { serialize(it) }
}

fun String.toMeldekortDager(): NonEmptyList<MeldekortDag> {
    return deserializeList<MeldekortDagDbJson>(this).map { it.toDomain() }.toNonEmptyListOrNull()!!
}
