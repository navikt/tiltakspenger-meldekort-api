package no.nav.tiltakspenger.meldekort.landingsside.infra.routes

import no.nav.tiltakspenger.meldekort.landingsside.LandingssideMeldekort
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideStatus
import java.time.LocalDateTime

/**
 *  [harInnsendteMeldekort] true dersom brukeren har sendt inn meldekort tidligere
 *  [meldekortTilUtfylling] Liste over meldekort som er klare til utfylling
 *  [redirectUrl] URL som lenkes til fra felles landingsside
 * */
interface LandingssideStatusKontrakt {
    val harInnsendteMeldekort: Boolean
    val meldekortTilUtfylling: List<LandingssideMeldekort>
    val redirectUrl: String

    /**
     *  [kanSendesFra] Tidspunkt der meldekortet blir tilgjengelig for innsending
     *  [kanFyllesUtFra] Tidspunkt der meldekortet blir tilgjengelig for utfylling
     *  [fristForInnsending] Siste frist for innsending av meldekort uten trekk i ytelsen
     * */
    interface LandingssideMeldekort {
        val kanSendesFra: LocalDateTime
        val kanFyllesUtFra: LocalDateTime?
        val fristForInnsending: LocalDateTime?
    }
}

fun LandingssideStatus.tilLandingssideStatusDTO(): LandingssideStatusDTO =
    LandingssideStatusDTO(
        harInnsendteMeldekort = harInnsendteMeldekort,
        meldekortTilUtfylling = meldekortTilUtfylling.map { it.tilLandingssideMeldekortDTO() },
        redirectUrl = redirectUrl,
    )

private fun LandingssideMeldekort.tilLandingssideMeldekortDTO(): LandingssideStatusDTO.LandingssideMeldekortDTO =
    LandingssideStatusDTO.LandingssideMeldekortDTO(
        kanSendesFra = kanSendesFra,
    )

data class LandingssideStatusDTO(
    override val harInnsendteMeldekort: Boolean,
    override val meldekortTilUtfylling: List<LandingssideMeldekortDTO>,
    override val redirectUrl: String,
) : LandingssideStatusKontrakt {

    /**
     *  [kanFyllesUtFra] Vi tillater utfylling og innsending fra samme tidspunkt
     *  [fristForInnsending] Vi har ingen frist for innsending nå, men dette kommer antagelig på plass når samtlige brukere er ute av Arena
     * */
    data class LandingssideMeldekortDTO(
        override val kanSendesFra: LocalDateTime,
    ) : LandingssideStatusKontrakt.LandingssideMeldekort {
        override val kanFyllesUtFra: LocalDateTime = kanSendesFra
        override val fristForInnsending: LocalDateTime? = null
    }
}
