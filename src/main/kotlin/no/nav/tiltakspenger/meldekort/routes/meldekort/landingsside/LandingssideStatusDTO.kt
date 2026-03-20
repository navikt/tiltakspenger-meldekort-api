package no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside

import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.LandingssideStatusKontrakt.LandingssideMeldekort
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
     *  [fristForInsending] Siste frist for innsending av meldekort uten trekk i ytelsen
     * */
    interface LandingssideMeldekort {
        val kanSendesFra: LocalDateTime
        val kanFyllesUtFra: LocalDateTime?
        val fristForInsending: LocalDateTime
    }
}

data class LandingssideStatusDTO(
    override val harInnsendteMeldekort: Boolean,
    override val meldekortTilUtfylling: List<LandingssideMeldekortDTO>,
) : LandingssideStatusKontrakt {
    override val redirectUrl: String = Configuration.meldekortFrontendUrl

    /**
     *  [kanFyllesUtFra] Vi tillater utfylling og innsending fra samme tidspunkt
     *  [fristForInsending] Vi har ingen frist for innsending nå, men dette kommer antagelig på plass når samtlige brukere er ute av Arena
     * */
    data class LandingssideMeldekortDTO(
        override val kanSendesFra: LocalDateTime,
    ) : LandingssideMeldekort {
        override val kanFyllesUtFra: LocalDateTime = kanSendesFra
        override val fristForInsending: LocalDateTime = kanSendesFra.plusYears(10)
    }
}
