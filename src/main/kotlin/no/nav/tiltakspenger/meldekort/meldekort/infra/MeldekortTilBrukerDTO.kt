package no.nav.tiltakspenger.meldekort.meldekort.infra

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDag
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortStatus
import no.nav.tiltakspenger.meldekort.meldekort.RegistrertMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortStatusDTO.Companion.toDTO
import no.nav.tiltakspenger.meldekort.meldekort.registrertTidspunkt
import no.nav.tiltakspenger.meldekort.utils.toNorskUkenummer
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortTilBrukerDTO(
    val id: String,
    val meldeperiodeId: String,
    val kjedeId: String,
    val versjon: Int,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val uke1: Int,
    val uke2: Int,
    val status: MeldekortStatusDTO,
    val maksAntallDager: Int,
    val innsendt: LocalDateTime?,
    val dager: List<MeldekortDagTilBrukerDTO>,
    val kanSendes: LocalDateTime?,
) {
    constructor(
        id: String,
        meldeperiodeId: String,
        kjedeId: String,
        versjon: Int,
        periode: Periode,
        uke1: Int,
        uke2: Int,
        status: MeldekortStatusDTO,
        maksAntallDager: Int,
        innsendt: LocalDateTime?,
        dager: List<MeldekortDagTilBrukerDTO>,
        kanSendes: LocalDateTime?,
    ) : this(
        id = id,
        meldeperiodeId = meldeperiodeId,
        kjedeId = kjedeId,
        versjon = versjon,
        fraOgMed = periode.fraOgMed,
        tilOgMed = periode.tilOgMed,
        uke1 = uke1,
        uke2 = uke2,
        status = status,
        maksAntallDager = maksAntallDager,
        innsendt = innsendt,
        dager = dager,
        kanSendes = kanSendes,
    )
}

enum class MeldekortStatusDTO {
    INNSENDT,
    KAN_UTFYLLES,
    IKKE_KLAR,
    DEAKTIVERT,
    ;

    companion object {
        fun MeldekortStatus.toDTO(): MeldekortStatusDTO = when (this) {
            MeldekortStatus.INNSENDT -> INNSENDT
            MeldekortStatus.KAN_UTFYLLES -> KAN_UTFYLLES
            MeldekortStatus.IKKE_KLAR -> IKKE_KLAR
            MeldekortStatus.DEAKTIVERT -> DEAKTIVERT
        }
    }
}

data class MeldekortDagTilBrukerDTO(
    val dag: LocalDate,
    val status: MeldekortDagStatusDTO,
)

fun BrukersMeldekort.tilMeldekortTilBrukerDTO(clock: Clock): MeldekortTilBrukerDTO {
    return MeldekortTilBrukerDTO(
        id = id.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        kjedeId = meldeperiode.kjedeId.toString(),
        versjon = meldeperiode.versjon,
        fraOgMed = periode.fraOgMed,
        tilOgMed = periode.tilOgMed,
        uke1 = periode.fraOgMed.toNorskUkenummer(),
        uke2 = periode.tilOgMed.toNorskUkenummer(),
        status = status(clock).toDTO(),
        maksAntallDager = meldeperiode.maksAntallDagerForPeriode,
        innsendt = mottatt,
        dager = dager.toDto(),
        kanSendes = klarTilInnsendingDateTime(clock),
    )
}

fun List<MeldekortDag>.toDto(): List<MeldekortDagTilBrukerDTO> {
    return this.map { dag ->
        MeldekortDagTilBrukerDTO(
            dag = dag.dag,
            status = dag.status.tilDTO(),
        )
    }
}

/**
 * Bygger DTO for en [RegistrertMeldekort] — kjedens registrerte («utfylte») tilstand,
 * uavhengig av om den kommer fra en digital innsending ([BrukersMeldekort]) eller et meldekortvedtak
 * ([VedtattMeldekort]). En registrert tilstand er per definisjon innsendt, så status er
 * [MeldekortStatusDTO.INNSENDT].
 */
fun RegistrertMeldekort.tilMeldekortTilBrukerDTO(): MeldekortTilBrukerDTO {
    return MeldekortTilBrukerDTO(
        id = id.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        kjedeId = meldeperiode.kjedeId.toString(),
        versjon = meldeperiode.versjon,
        fraOgMed = meldeperiode.periode.fraOgMed,
        tilOgMed = meldeperiode.periode.tilOgMed,
        uke1 = meldeperiode.periode.fraOgMed.toNorskUkenummer(),
        uke2 = meldeperiode.periode.tilOgMed.toNorskUkenummer(),
        status = MeldekortStatusDTO.INNSENDT,
        maksAntallDager = meldeperiode.maksAntallDagerForPeriode,
        innsendt = registrertTidspunkt(),
        dager = dager.toDto(),
        kanSendes = null,
    )
}
