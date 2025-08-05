package no.nav.tiltakspenger.meldekort.repository

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagFraBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatusDTO
import java.time.LocalDate

private data class MeldekortDagDbJson(
    val dag: LocalDate,
    val status: MeldekortDagDbStatus,
) {
    @Suppress("EnumEntryName")
    enum class MeldekortDagDbStatus {
        DELTATT_UTEN_LØNN_I_TILTAKET,
        DELTATT_MED_LØNN_I_TILTAKET,
        FRAVÆR_SYK,
        FRAVÆR_SYKT_BARN,
        FRAVÆR_GODKJENT_AV_NAV,
        FRAVÆR_ANNET,
        IKKE_BESVART,
        IKKE_TILTAKSDAG,
        IKKE_RETT_TIL_TILTAKSPENGER,
    }

    fun toDomain(): MeldekortDag {
        return MeldekortDag(
            dag = dag,
            status = when (status) {
                MeldekortDagDbStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
                MeldekortDagDbStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
                MeldekortDagDbStatus.FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
                MeldekortDagDbStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
                MeldekortDagDbStatus.FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV
                MeldekortDagDbStatus.FRAVÆR_ANNET -> MeldekortDagStatus.FRAVÆR_ANNET
                MeldekortDagDbStatus.IKKE_BESVART -> MeldekortDagStatus.IKKE_BESVART
                MeldekortDagDbStatus.IKKE_TILTAKSDAG -> MeldekortDagStatus.IKKE_TILTAKSDAG
                MeldekortDagDbStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
            },
        )
    }
}

fun List<MeldekortDag>.tilMeldekortDagDbJson(): String {
    return this.map { dag ->
        MeldekortDagDbJson(
            dag = dag.dag,
            status = when (dag.status) {
                MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagDbJson.MeldekortDagDbStatus.DELTATT_UTEN_LØNN_I_TILTAKET
                MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagDbJson.MeldekortDagDbStatus.DELTATT_MED_LØNN_I_TILTAKET
                MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_SYK
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_SYKT_BARN
                MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_GODKJENT_AV_NAV
                MeldekortDagStatus.FRAVÆR_ANNET -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_ANNET
                MeldekortDagStatus.IKKE_BESVART -> MeldekortDagDbJson.MeldekortDagDbStatus.IKKE_BESVART
                MeldekortDagStatus.IKKE_TILTAKSDAG -> MeldekortDagDbJson.MeldekortDagDbStatus.IKKE_TILTAKSDAG
                MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagDbJson.MeldekortDagDbStatus.IKKE_RETT_TIL_TILTAKSPENGER
            },
        )
    }.let { serialize(it) }
}

fun List<MeldekortDagFraBrukerDTO>.tilMeldekortDagFraBrukerDbJson(): String {
    return this.map { dag ->
        MeldekortDagDbJson(
            dag = dag.dag,
            status = when (dag.status) {
                MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagDbJson.MeldekortDagDbStatus.DELTATT_UTEN_LØNN_I_TILTAKET
                MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagDbJson.MeldekortDagDbStatus.DELTATT_MED_LØNN_I_TILTAKET
                MeldekortDagStatusDTO.FRAVÆR_SYK -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_SYK
                MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_SYKT_BARN
                MeldekortDagStatusDTO.FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_GODKJENT_AV_NAV
                MeldekortDagStatusDTO.FRAVÆR_ANNET -> MeldekortDagDbJson.MeldekortDagDbStatus.FRAVÆR_ANNET
                MeldekortDagStatusDTO.IKKE_BESVART -> MeldekortDagDbJson.MeldekortDagDbStatus.IKKE_BESVART
                MeldekortDagStatusDTO.IKKE_TILTAKSDAG -> MeldekortDagDbJson.MeldekortDagDbStatus.IKKE_TILTAKSDAG
                MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagDbJson.MeldekortDagDbStatus.IKKE_RETT_TIL_TILTAKSPENGER
            },
        )
    }.let { serialize(it) }
}

fun String.toMeldekortDager(): NonEmptyList<MeldekortDag> {
    return deserializeList<MeldekortDagDbJson>(this).map { it.toDomain() }.toNonEmptyListOrNull()!!
}
