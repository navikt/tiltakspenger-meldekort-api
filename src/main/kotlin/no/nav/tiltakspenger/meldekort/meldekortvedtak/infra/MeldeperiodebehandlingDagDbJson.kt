package no.nav.tiltakspenger.meldekort.meldekortvedtak.infra

import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldeperiodebehandlingDag
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon
import java.time.LocalDate

private data class DagDbJson(
    val dato: LocalDate,
    val status: StatusDb,
    val reduksjon: ReduksjonDb,
    val beløp: Int,
    val beløpBarnetillegg: Int,
) {
    @Suppress("EnumEntryName")
    enum class StatusDb {
        DELTATT_UTEN_LØNN_I_TILTAKET,
        DELTATT_MED_LØNN_I_TILTAKET,
        FRAVÆR_SYK,
        FRAVÆR_SYKT_BARN,
        FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
        FRAVÆR_GODKJENT_AV_NAV,
        FRAVÆR_ANNET,
        IKKE_BESVART,
        IKKE_TILTAKSDAG,
        IKKE_RETT_TIL_TILTAKSPENGER,
    }

    enum class ReduksjonDb {
        INGEN_REDUKSJON,
        REDUKSJON,
        YTELSEN_FALLER_BORT,
    }

    fun toDomain(): MeldeperiodebehandlingDag = MeldeperiodebehandlingDag(
        dato = dato,
        status = when (status) {
            StatusDb.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
            StatusDb.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
            StatusDb.FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
            StatusDb.FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
            StatusDb.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU -> MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU
            StatusDb.FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV
            StatusDb.FRAVÆR_ANNET -> MeldekortDagStatus.FRAVÆR_ANNET
            StatusDb.IKKE_BESVART -> MeldekortDagStatus.IKKE_BESVART
            StatusDb.IKKE_TILTAKSDAG -> MeldekortDagStatus.IKKE_TILTAKSDAG
            StatusDb.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
        },
        reduksjon = when (reduksjon) {
            ReduksjonDb.INGEN_REDUKSJON -> Reduksjon.INGEN_REDUKSJON
            ReduksjonDb.REDUKSJON -> Reduksjon.REDUKSJON
            ReduksjonDb.YTELSEN_FALLER_BORT -> Reduksjon.YTELSEN_FALLER_BORT
        },
        beløp = beløp,
        beløpBarnetillegg = beløpBarnetillegg,
    )
}

fun List<MeldeperiodebehandlingDag>.tilDagerDbJson(): String =
    serialize(
        this.map { dag ->
            DagDbJson(
                dato = dag.dato,
                status = when (dag.status) {
                    MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> DagDbJson.StatusDb.DELTATT_UTEN_LØNN_I_TILTAKET
                    MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> DagDbJson.StatusDb.DELTATT_MED_LØNN_I_TILTAKET
                    MeldekortDagStatus.FRAVÆR_SYK -> DagDbJson.StatusDb.FRAVÆR_SYK
                    MeldekortDagStatus.FRAVÆR_SYKT_BARN -> DagDbJson.StatusDb.FRAVÆR_SYKT_BARN
                    MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU -> DagDbJson.StatusDb.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU
                    MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> DagDbJson.StatusDb.FRAVÆR_GODKJENT_AV_NAV
                    MeldekortDagStatus.FRAVÆR_ANNET -> DagDbJson.StatusDb.FRAVÆR_ANNET
                    MeldekortDagStatus.IKKE_BESVART -> DagDbJson.StatusDb.IKKE_BESVART
                    MeldekortDagStatus.IKKE_TILTAKSDAG -> DagDbJson.StatusDb.IKKE_TILTAKSDAG
                    MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> DagDbJson.StatusDb.IKKE_RETT_TIL_TILTAKSPENGER
                },
                reduksjon = when (dag.reduksjon) {
                    Reduksjon.INGEN_REDUKSJON -> DagDbJson.ReduksjonDb.INGEN_REDUKSJON
                    Reduksjon.REDUKSJON -> DagDbJson.ReduksjonDb.REDUKSJON
                    Reduksjon.YTELSEN_FALLER_BORT -> DagDbJson.ReduksjonDb.YTELSEN_FALLER_BORT
                },
                beløp = dag.beløp,
                beløpBarnetillegg = dag.beløpBarnetillegg,
            )
        },
    )

fun String.tilMeldeperiodebehandlingDager(): List<MeldeperiodebehandlingDag> =
    deserializeList<DagDbJson>(this).map { it.toDomain() }
