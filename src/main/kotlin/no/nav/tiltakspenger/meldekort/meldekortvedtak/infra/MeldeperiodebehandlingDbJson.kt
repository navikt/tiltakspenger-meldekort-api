package no.nav.tiltakspenger.meldekort.meldekortvedtak.infra

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.deserializeList
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldeperiodebehandling
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldeperiodebehandlingDag
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon
import java.time.LocalDate

private data class MeldeperiodebehandlingDbJson(
    val meldeperiodeId: String,
    val meldeperiodeKjedeId: String,
    val brukersMeldekortId: String?,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val dager: List<DagDbJson>,
) {
    data class DagDbJson(
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

    fun toDomain(): Meldeperiodebehandling = Meldeperiodebehandling(
        meldeperiodeId = MeldeperiodeId.fromString(meldeperiodeId),
        meldeperiodeKjedeId = MeldeperiodeKjedeId(meldeperiodeKjedeId),
        brukersMeldekortId = brukersMeldekortId?.let { MeldekortId.fromString(it) },
        periode = Periode(fraOgMed = fraOgMed, tilOgMed = tilOgMed),
        dager = dager.map { it.toDomain() },
    )
}

fun List<Meldeperiodebehandling>.tilMeldeperiodebehandlingerDbJson(): String =
    serialize(
        this.map { behandling ->
            MeldeperiodebehandlingDbJson(
                meldeperiodeId = behandling.meldeperiodeId.toString(),
                meldeperiodeKjedeId = behandling.meldeperiodeKjedeId.toString(),
                brukersMeldekortId = behandling.brukersMeldekortId?.toString(),
                fraOgMed = behandling.periode.fraOgMed,
                tilOgMed = behandling.periode.tilOgMed,
                dager = behandling.dager.map { dag ->
                    MeldeperiodebehandlingDbJson.DagDbJson(
                        dato = dag.dato,
                        status = when (dag.status) {
                            MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldeperiodebehandlingDbJson.DagDbJson.StatusDb.DELTATT_UTEN_LØNN_I_TILTAKET
                            MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldeperiodebehandlingDbJson.DagDbJson.StatusDb.DELTATT_MED_LØNN_I_TILTAKET
                            MeldekortDagStatus.FRAVÆR_SYK -> MeldeperiodebehandlingDbJson.DagDbJson.StatusDb.FRAVÆR_SYK
                            MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldeperiodebehandlingDbJson.DagDbJson.StatusDb.FRAVÆR_SYKT_BARN
                            MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU -> MeldeperiodebehandlingDbJson.DagDbJson.StatusDb.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU
                            MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> MeldeperiodebehandlingDbJson.DagDbJson.StatusDb.FRAVÆR_GODKJENT_AV_NAV
                            MeldekortDagStatus.FRAVÆR_ANNET -> MeldeperiodebehandlingDbJson.DagDbJson.StatusDb.FRAVÆR_ANNET
                            MeldekortDagStatus.IKKE_BESVART -> MeldeperiodebehandlingDbJson.DagDbJson.StatusDb.IKKE_BESVART
                            MeldekortDagStatus.IKKE_TILTAKSDAG -> MeldeperiodebehandlingDbJson.DagDbJson.StatusDb.IKKE_TILTAKSDAG
                            MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldeperiodebehandlingDbJson.DagDbJson.StatusDb.IKKE_RETT_TIL_TILTAKSPENGER
                        },
                        reduksjon = when (dag.reduksjon) {
                            Reduksjon.INGEN_REDUKSJON -> MeldeperiodebehandlingDbJson.DagDbJson.ReduksjonDb.INGEN_REDUKSJON
                            Reduksjon.REDUKSJON -> MeldeperiodebehandlingDbJson.DagDbJson.ReduksjonDb.REDUKSJON
                            Reduksjon.YTELSEN_FALLER_BORT -> MeldeperiodebehandlingDbJson.DagDbJson.ReduksjonDb.YTELSEN_FALLER_BORT
                        },
                        beløp = dag.beløp,
                        beløpBarnetillegg = dag.beløpBarnetillegg,
                    )
                },
            )
        },
    )

fun String.tilMeldeperiodebehandlinger(): List<Meldeperiodebehandling> =
    deserializeList<MeldeperiodebehandlingDbJson>(this).map { it.toDomain() }
