package no.nav.tiltakspenger.meldekort.journalføring

import java.util.Base64

/**
 * TODO jah: Flytt til libs.
 */
data class PdfOgJson(
    val pdf: PdfA,
    val json: String,
) {
    fun pdfAsBase64(): String = pdf.toBase64()
    fun jsonAsBase64(): String = Base64.getEncoder().encodeToString(json.toByteArray())
}
