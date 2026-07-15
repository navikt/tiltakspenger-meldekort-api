package no.nav.tiltakspenger.meldekort.journalføring.infra

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDag
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.utils.toEngelskDato
import no.nav.tiltakspenger.meldekort.utils.toEngelskDatoOgTid
import no.nav.tiltakspenger.meldekort.utils.toEngelskUkedagOgDatoUtenÅr
import no.nav.tiltakspenger.meldekort.utils.toEngelskUkenummer
import no.nav.tiltakspenger.meldekort.utils.toNorskDato
import no.nav.tiltakspenger.meldekort.utils.toNorskDatoOgTid
import no.nav.tiltakspenger.meldekort.utils.toNorskUkedagOgDatoUtenÅr
import no.nav.tiltakspenger.meldekort.utils.toNorskUkenummer

/**
 * Serialiserer et [BrukersMeldekort] til JSON-en som sendes til tiltakspenger-pdfgen for å generere brevet (PDF).
 *
 * Dette er den eneste offentlige inngangen til brev-DTO-ene.
 * Selve DTO-ene er private slik at de ikke lekker ut av denne fila, og slik at koblingen mot domenet (`BrukersMeldekort`/`MeldekortDag`/`MeldekortDagStatus`) holdes innenfor mappingen her.
 *
 * Datoer formateres her fordi vi ikke bruker tid på å opprette hjelpemetoder i pdfgen-core per nå.
 */
fun BrukersMeldekort.toDTO(): String = serialize(this.toBrevMeldekortDTO())

private data class BrevMeldekortDTO(
    val id: String,
    val fnr: String,
    val periode: PeriodeDTO,
    val uke1: Int,
    val uke2: Int,
    val dager: List<BrevMeldekortDagDTO>,
    val saksnummer: String? = null,
    val mottatt: String?,
)

private data class BrevMeldekortDagDTO(
    val dag: String,
    val status: BrevMeldekortStatusDTO,
)

private enum class BrevMeldekortStatusDTO {
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

private fun BrukersMeldekort.toBrevMeldekortDTO(): BrevMeldekortDTO {
    val engelsk = this.locale == "en"
    return BrevMeldekortDTO(
        id = this.id.toString(),
        fnr = this.fnr.verdi,
        periode = if (engelsk) {
            PeriodeDTO(this.periode.fraOgMed.toEngelskDato(), this.periode.tilOgMed.toEngelskDato())
        } else {
            PeriodeDTO(this.periode.fraOgMed.toNorskDato(), this.periode.tilOgMed.toNorskDato())
        },
        uke1 = if (engelsk) this.periode.fraOgMed.toEngelskUkenummer() else this.periode.fraOgMed.toNorskUkenummer(),
        uke2 = if (engelsk) this.periode.tilOgMed.toEngelskUkenummer() else this.periode.tilOgMed.toNorskUkenummer(),
        dager = this.dager.tilBrevMeldekortDagDTO(engelsk),
        saksnummer = this.meldeperiode.saksnummer,
        mottatt = if (engelsk) this.mottatt?.toEngelskDatoOgTid() else this.mottatt?.toNorskDatoOgTid(),
    )
}

private fun List<MeldekortDag>.tilBrevMeldekortDagDTO(engelsk: Boolean): List<BrevMeldekortDagDTO> = this.map {
    BrevMeldekortDagDTO(
        dag = if (engelsk) it.dag.toEngelskUkedagOgDatoUtenÅr() else it.dag.toNorskUkedagOgDatoUtenÅr(),
        status = it.status.tilBrevMeldekortStatusDTO(),
    )
}

private fun MeldekortDagStatus.tilBrevMeldekortStatusDTO(): BrevMeldekortStatusDTO = when (this) {
    MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> BrevMeldekortStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
    MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> BrevMeldekortStatusDTO.DELTATT_MED_LØNN_I_TILTAKET
    MeldekortDagStatus.FRAVÆR_SYK -> BrevMeldekortStatusDTO.FRAVÆR_SYK
    MeldekortDagStatus.FRAVÆR_SYKT_BARN -> BrevMeldekortStatusDTO.FRAVÆR_SYKT_BARN
    MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU -> BrevMeldekortStatusDTO.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU
    MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> BrevMeldekortStatusDTO.FRAVÆR_GODKJENT_AV_NAV
    MeldekortDagStatus.FRAVÆR_ANNET -> BrevMeldekortStatusDTO.FRAVÆR_ANNET
    MeldekortDagStatus.IKKE_BESVART -> BrevMeldekortStatusDTO.IKKE_BESVART
    MeldekortDagStatus.IKKE_TILTAKSDAG -> BrevMeldekortStatusDTO.IKKE_TILTAKSDAG
    MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> BrevMeldekortStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
}
