import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.meldekort.clients.pdfgen.BrevMeldekortDagDTO
import no.nav.tiltakspenger.meldekort.clients.pdfgen.toDTO
import no.nav.tiltakspenger.meldekort.clients.utils.toNorskDato
import no.nav.tiltakspenger.meldekort.clients.utils.toNorskDatoOgTid
import no.nav.tiltakspenger.meldekort.clients.utils.toNorskUkenummer
import no.nav.tiltakspenger.meldekort.domene.Meldekort

/**
 * DTO for å serialisere meldekort til brev (PDF). Datoer formateres her fordi vi ikke bruker tid på å opprette hjelpemetoder i pdfgen-core per nå
 */
class BrevMeldekortDTO(
    val id: String,
    val fnr: String,
    val periode: PeriodeDTO,
    val uke1: Int,
    val uke2: Int,
    val dager: List<BrevMeldekortDagDTO>,
    val saksnummer: String? = null,
    val mottatt: String?,
)

internal fun Meldekort.toBrevMeldekortDTO(): String {
    return BrevMeldekortDTO(
        id = this.id.toString(),
        fnr = this.fnr.verdi,
        periode = PeriodeDTO(this.periode.fraOgMed.toNorskDato(), this.periode.tilOgMed.toNorskDato()),
        uke1 = this.periode.fraOgMed.toNorskUkenummer(),
        uke2 = this.periode.tilOgMed.toNorskUkenummer(),
        dager = this.dager.toDTO(),
        saksnummer = this.meldeperiode.saksnummer,
        mottatt = this.mottatt?.toNorskDatoOgTid(),
    ).let { serialize(it) }
}
