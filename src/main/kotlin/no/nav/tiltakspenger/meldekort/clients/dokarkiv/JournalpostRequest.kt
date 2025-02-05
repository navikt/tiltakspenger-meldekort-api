package no.nav.tiltakspenger.meldekort.clients.dokarkiv

import no.nav.tiltakspenger.meldekort.clients.utils.toNorskDatoMedPunktum
import no.nav.tiltakspenger.meldekort.clients.utils.toNorskUkenummer
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.journalføring.PdfOgJson
import java.time.LocalDateTime

enum class Tema(val value: String) {
    TILTAKSPENGER(INDIVIDSTONAD),
}

const val JOURNALFORENDE_ENHET_AUTOMATISK_BEHANDLING = "9999"
const val MELDEKORT_BREVKODE = "NAV 00-10.02"

data class JournalpostRequest(
    val tittel: String,
    val journalpostType: JournalPostType = JournalPostType.INNGAAENDE,
    val tema: String = Tema.TILTAKSPENGER.value,
    val kanal: String = "NAV_NO",
    val journalfoerendeEnhet: String?,
    val avsenderMottaker: AvsenderMottaker,
    val bruker: Bruker,
    val sak: Sak?,
    val datoMottatt: LocalDateTime,
    val dokumenter: List<JournalpostDokument>,
    val eksternReferanseId: String,
) {
    fun kanFerdigstilleAutomatisk() =
        !journalfoerendeEnhet.isNullOrEmpty() && !sak?.fagsakId.isNullOrEmpty()
}

fun Meldekort.toJournalpostDokument(
    journalforendeEnhet: String? = JOURNALFORENDE_ENHET_AUTOMATISK_BEHANDLING,
    pdfOgJson: PdfOgJson,
): JournalpostRequest {
    val tittel = this.lagTittel()
    return JournalpostRequest(
        tittel = tittel,
        journalfoerendeEnhet = journalforendeEnhet,
        datoMottatt = this.mottatt ?: LocalDateTime.now(),
        avsenderMottaker = AvsenderMottaker(id = fnr.verdi),
        bruker = Bruker(id = fnr.verdi),
        sak = this.meldeperiode.saksnummer?.let {
            Sak(fagsakId = it)
        },
        dokumenter = listOf(
            JournalpostDokument(
                tittel = tittel,
                brevkode = MELDEKORT_BREVKODE,
                dokumentvarianter = listOf(
                    DokumentVariant.ArkivPDF(
                        fysiskDokument = pdfOgJson.pdfAsBase64(),
                    ),
                    DokumentVariant.OriginalJson(
                        fysiskDokument = pdfOgJson.jsonAsBase64(),
                    ),
                ),
            ),
        ),
        eksternReferanseId = this.id.toString(),
    )
}

private fun Meldekort.lagTittel(): String {
    val uke1 = this.meldeperiode.periode.fraOgMed.toNorskUkenummer()
    val uke2 = this.meldeperiode.periode.tilOgMed.toNorskUkenummer()
    val fra = this.meldeperiode.periode.fraOgMed.toNorskDatoMedPunktum()
    val til = this.meldeperiode.periode.tilOgMed.toNorskDatoMedPunktum()

    val tittel = "Meldekort"
    return "$tittel for uke $uke1 - $uke2 ($fra - $til) elektronisk mottatt av Nav"
}

data class AvsenderMottaker(
    val id: String,
    val idType: String = "FNR",
)

data class Bruker(
    val id: String,
    val idType: String = "FNR",
)

data class Sak(
    val fagsakId: String,
    val fagsaksystem: String = "TILTAKSPENGER",
    val sakstype: String = "FAGSAK",
)

data class JournalpostDokument(
    val tittel: String,
    val brevkode: String?,
    val dokumentvarianter: List<DokumentVariant>,
)

sealed class DokumentVariant {
    abstract val filtype: String
    abstract val fysiskDokument: String
    abstract val variantformat: String
    abstract val filnavn: String

    data class ArkivPDF(
        override val fysiskDokument: String,
    ) : DokumentVariant() {
        override val filtype: String = "PDFA"
        override val variantformat: String = "ARKIV"
        override val filnavn: String = "meldekort.pdf"
    }

    data class OriginalJson(
        override val fysiskDokument: String,
    ) : DokumentVariant() {
        override val filtype: String = "JSON"
        override val variantformat: String = "ORIGINAL"
        override val filnavn: String = "meldekort.json"
    }
}

enum class JournalPostType(val type: String) {
    INNGAAENDE("INNGAAENDE"),
    UTGAAENDE("UTGAAENDE"),
}
