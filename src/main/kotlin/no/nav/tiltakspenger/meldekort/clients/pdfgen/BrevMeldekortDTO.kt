import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.meldekort.clients.pdfgen.BrevMeldekortDagDTO
import no.nav.tiltakspenger.meldekort.clients.pdfgen.tilBrevMeldekortDagDTO
import no.nav.tiltakspenger.meldekort.clients.utils.toEngelskDato
import no.nav.tiltakspenger.meldekort.clients.utils.toEngelskDatoOgTid
import no.nav.tiltakspenger.meldekort.clients.utils.toEngelskUkenummer
import no.nav.tiltakspenger.meldekort.clients.utils.toNorskDato
import no.nav.tiltakspenger.meldekort.clients.utils.toNorskDatoOgTid
import no.nav.tiltakspenger.meldekort.clients.utils.toNorskUkenummer
import no.nav.tiltakspenger.meldekort.domene.Meldekort

/**
 * DTO for å serialisere meldekort til brev (PDF). Datoer formateres her fordi vi ikke bruker tid på å opprette hjelpemetoder i pdfgen-core per nå
 */
data class BrevMeldekortDTO(
    val id: String,
    val fnr: String,
    val periode: PeriodeDTO,
    val uke1: Int,
    val uke2: Int,
    val dager: List<BrevMeldekortDagDTO>,
    val saksnummer: String? = null,
    val mottatt: String?,
)

fun Meldekort.toBrevMeldekortDTO(): String {
    val dto = if (this.locale == "en") {
        this.toEngelskBrevMeldekortDTO()
    } else {
        this.toNorskBrevMeldekortDTO()
    }
    return serialize(dto)
}

fun Meldekort.toNorskBrevMeldekortDTO(): BrevMeldekortDTO {
    return BrevMeldekortDTO(
        id = this.id.toString(),
        fnr = this.fnr.verdi,
        periode = PeriodeDTO(this.periode.fraOgMed.toNorskDato(), this.periode.tilOgMed.toNorskDato()),
        uke1 = this.periode.fraOgMed.toNorskUkenummer(),
        uke2 = this.periode.tilOgMed.toNorskUkenummer(),
        dager = this.dager.tilBrevMeldekortDagDTO(null),
        saksnummer = this.meldeperiode.saksnummer,
        mottatt = this.mottatt?.toNorskDatoOgTid(),
    )
}

fun Meldekort.toEngelskBrevMeldekortDTO(): BrevMeldekortDTO {
    return BrevMeldekortDTO(
        id = this.id.toString(),
        fnr = this.fnr.verdi,
        periode = PeriodeDTO(this.periode.fraOgMed.toEngelskDato(), this.periode.tilOgMed.toEngelskDato()),
        uke1 = this.periode.fraOgMed.toEngelskUkenummer(),
        uke2 = this.periode.tilOgMed.toEngelskUkenummer(),
        dager = this.dager.tilBrevMeldekortDagDTO("en"),
        saksnummer = this.meldeperiode.saksnummer,
        mottatt = this.mottatt?.toEngelskDatoOgTid(),
    )
}
