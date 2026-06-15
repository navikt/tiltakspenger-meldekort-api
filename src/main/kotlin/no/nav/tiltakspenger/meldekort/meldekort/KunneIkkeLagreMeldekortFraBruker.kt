package no.nav.tiltakspenger.meldekort.meldekort

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import java.time.LocalDateTime

/**
 * Forventede feil ved innsending av meldekort fra bruker (se [LagreMeldekortFraBrukerService]).
 *
 * Typen samler hele forståelsen av hvordan en feil skal tolkes: hva som har skjedd, hva bruker kan forvente, om
 * bruker kan prøve å sende inn på nytt ([kanPrøveIgjen]), hvor alvorlig feilen er for oss som drifter tjenesten
 * ([loggnivå]), og om det finnes potensielt sensitive detaljer som kun skal til sikkerlogg ([throwable]).
 * Den brukervendte teksten og HTTP-statusen ligger derimot i route-laget (`SendInnMeldekortRoute.toErrorJson`),
 * siden det er en transport-/presentasjonsdetalj.
 *
 * [loggMelding] er bevisst fri for personopplysninger (fnr, utfylte dager, stedinformasjon e.l.) slik at den
 * trygt kan logges i vanlig logg. Potensielt sensitive detaljer (f.eks. [UventetFeilVedLagring.throwable])
 * skal kun til sikkerlogg.
 */
sealed interface KunneIkkeLagreMeldekortFraBruker {
    /**
     * Melding trygg for vanlig logg. Inneholder interne IDer, men ingen personopplysninger. Settes sammen av
     * [beskrivelse] og en teknisk markør for om bruker kan prøve igjen ([kanPrøveIgjen]), slik at loggen alene
     * gir en komplett tolkning av feilen.
     */
    val loggMelding: String
        get() = buildString {
            append("$beskrivelse (kanPrøveIgjen: $kanPrøveIgjen")
            kanPrøveIgjenTidspunkt?.let { append(", kanPrøveIgjenTidspunkt: $it") }
            append(")")
        }

    /** Kjernebeskrivelsen av hva som gikk galt. Fri for personopplysninger, men kan inneholde interne IDer. */
    val beskrivelse: String

    /**
     * Om bruker kan forvente at et nytt forsøk på å sende inn det samme meldekortet kan lykkes.
     *
     * `true` betyr at feilen er forbigående eller tidsavhengig (f.eks. teknisk feil eller at meldekortet ennå
     * ikke er klart). `false` betyr at et nytt forsøk på det samme meldekortet gir samme resultat, og at bruker
     * må gjøre noe annet (f.eks. gå til oversikten og velge et annet/nyere meldekort) eller kontakte oss.
     */
    val kanPrøveIgjen: Boolean

    /**
     * Hvor alvorlig feilen er for oss som drifter tjenesten, og dermed hvilket nivå [loggMelding] skal logges på
     * i vanlig logg.
     *
     * [Loggnivå.WARN] brukes for forventede feil som typisk skyldes brukerens situasjon eller timing (4xx), og
     * som ikke krever at vi gjør noe. [Loggnivå.ERROR] brukes for feil som ikke skal kunne oppstå eller som er
     * uventede (5xx), og som vi bør undersøke.
     */
    val loggnivå: Loggnivå

    /**
     * Eventuell underliggende feil som kan inneholde personopplysninger og derfor kun skal logges til
     * sikkerlogg. `null` for feil der hele konteksten allerede er fanget trygt i [loggMelding].
     */
    val throwable: Throwable? get() = null

    /**
     * Tidspunktet bruker tidligst kan forvente at et nytt forsøk lykkes, der vi kjenner det (f.eks. når
     * meldekortet først kan fylles ut). `null` når feilen ikke er tidsavhengig eller vi ikke har et konkret
     * tidspunkt å oppgi.
     */
    val kanPrøveIgjenTidspunkt: LocalDateTime? get() = null

    /**
     * Meldekortet finnes ikke for den innloggede brukeren (feil/ukjent id, eller meldekortet tilhører en annen
     * person). Bruker bør gå tilbake til oversikten og velge meldekortet på nytt. Et nytt forsøk på den samme
     * innsendingen vil ikke hjelpe.
     *
     * Logges som [Loggnivå.WARN]: oppslaget gjøres på `meldekortId` fra request-body, som er brukerkontrollert
     * input. En oppdiktet eller utdatert id kan derfor provosere denne uten at det er en feil hos oss, og vi kan
     * ikke skille det fra et reelt problem. Vi velger derfor det minst alarmerende nivået.
     */
    data class FantIkkeMeldekort(val meldekortId: MeldekortId) : KunneIkkeLagreMeldekortFraBruker {
        override val beskrivelse: String =
            "Fant ikke meldekort med id $meldekortId for innsendende bruker."
        override val kanPrøveIgjen: Boolean = false
        override val loggnivå: Loggnivå = Loggnivå.WARN
    }

    /**
     * Meldekortet finnes, men vi finner ingen gjeldende meldeperiode for kjeden. Dette er en intern
     * inkonsistens som ikke skal kunne oppstå, og som et nytt forsøk ikke retter opp. Bruker bør kontakte oss
     * dersom problemet vedvarer.
     *
     * Logges som [Loggnivå.ERROR] (i motsetning til [FantIkkeMeldekort]): oppslaget gjøres på `kjedeId` avledet
     * fra det allerede lagrede meldekortet — ikke fra request-body — så brukeren kan ikke styre eller provosere
     * det. Når den feiler, peker vårt eget meldekort på en kjede uten meldeperiode, altså en feil i våre data
     * eller logikk som vi bør undersøke.
     */
    data class FantIkkeMeldeperiode(
        val meldekortId: MeldekortId,
        val kjedeId: MeldeperiodeKjedeId,
    ) : KunneIkkeLagreMeldekortFraBruker {
        override val beskrivelse: String =
            "Fant ingen meldeperiode for kjedeId $kjedeId ved innsending av meldekort med id $meldekortId."
        override val kanPrøveIgjen: Boolean = false
        override val loggnivå: Loggnivå = Loggnivå.ERROR
    }

    /**
     * Meldekortet er allerede mottatt/sendt inn tidligere. Innsendingen er altså allerede registrert, og et nytt
     * forsøk vil gi samme svar.
     *
     * Logges som [Loggnivå.WARN]: en forventet situasjon (typisk dobbel innsending) som ikke krever oppfølging.
     */
    data class MeldekortErAlleredeMottatt(val meldekortId: MeldekortId) : KunneIkkeLagreMeldekortFraBruker {
        override val beskrivelse: String =
            "Meldekort med id $meldekortId er allerede mottatt og kan ikke sendes inn på nytt."
        override val kanPrøveIgjen: Boolean = false
        override val loggnivå: Loggnivå = Loggnivå.WARN
    }

    /**
     * Meldeperioden meldekortet ble fylt ut for er erstattet av en nyere versjon (typisk pga. en revurdering).
     * Dette meldekortet er ikke lenger gyldig. Bruker må gå tilbake til oversikten og sende inn det nyeste
     * meldekortet i stedet — å prøve dette på nytt hjelper ikke.
     *
     * Logges som [Loggnivå.WARN]: en forventet konsekvens av at meldeperioden er oppdatert, ikke en feil hos oss.
     */
    data class MeldekortetsMeldeperiodeErErstattet(
        val meldekortId: MeldekortId,
        val meldekortetsMeldeperiodeId: MeldeperiodeId,
        val sisteMeldeperiodeId: MeldeperiodeId,
    ) : KunneIkkeLagreMeldekortFraBruker {
        override val beskrivelse: String =
            "Meldekortets meldeperiode ($meldekortetsMeldeperiodeId) er erstattet av en nyere meldeperiode " +
                "($sisteMeldeperiodeId). Meldekort med id $meldekortId kan derfor ikke sendes inn."
        override val kanPrøveIgjen: Boolean = false
        override val loggnivå: Loggnivå = Loggnivå.WARN
    }

    /**
     * Meldekortet er deaktivert. Et meldekort deaktiveres når meldeperioden får en ny versjon (typisk pga. en
     * revurdering) før dette meldekortet er sendt inn, og det blir aldri utfyllbart igjen. Dette er samme
     * bruker-situasjon som [MeldekortetsMeldeperiodeErErstattet]: bruker må gå tilbake til oversikten og sende
     * inn det nyeste meldekortet — å prøve dette på nytt hjelper ikke.
     *
     * Logges som [Loggnivå.WARN]: en forventet konsekvens av at meldeperioden er oppdatert. I praksis fanges
     * dette normalt allerede av [MeldekortetsMeldeperiodeErErstattet] (deaktivering henger sammen med en nyere
     * meldeperiode-versjon); denne varianten finnes for at statushåndteringen skal være eksplisitt og robust.
     */
    data class MeldekortErDeaktivert(val meldekortId: MeldekortId) : KunneIkkeLagreMeldekortFraBruker {
        override val beskrivelse: String =
            "Meldekort med id $meldekortId er deaktivert og kan ikke sendes inn."
        override val kanPrøveIgjen: Boolean = false
        override val loggnivå: Loggnivå = Loggnivå.WARN
    }

    /**
     * Meldekortet er ikke klart til innsending ennå (det kan f.eks. ikke fylles ut før meldeperioden er over).
     * Dette er en tidsavhengig tilstand: bruker kan prøve igjen fra og med [kanPrøveIgjenTidspunkt].
     *
     * Logges som [Loggnivå.WARN]: en forventet timing-situasjon som løser seg selv når tiden er inne.
     */
    data class MeldekortErIkkeKlartTilInnsending(
        val meldekortId: MeldekortId,
        val status: MeldekortStatus,
        override val kanPrøveIgjenTidspunkt: LocalDateTime,
    ) : KunneIkkeLagreMeldekortFraBruker {
        override val beskrivelse: String =
            "Meldekort med id $meldekortId er ikke klart til innsending (status: $status)."
        override val kanPrøveIgjen: Boolean = true
        override val loggnivå: Loggnivå = Loggnivå.WARN
    }

    /**
     * Uventet feil ved selve lagringen/utfyllingen (f.eks. brudd på en domeneinvariant eller en feil mot
     * databasen). Feilen kan være forbigående, så bruker kan prøve å sende inn på nytt.
     *
     * Logges som [Loggnivå.ERROR]: en uventet feil vi bør undersøke. [throwable] kan inneholde personopplysninger
     * (f.eks. utfylte dager) og skal derfor kun logges til sikkerlogg. [loggMelding] er trygg for vanlig logg.
     */
    data class UventetFeilVedLagring(
        val meldekortId: MeldekortId,
        override val throwable: Throwable,
    ) : KunneIkkeLagreMeldekortFraBruker {
        override val beskrivelse: String =
            "Uventet feil ved lagring av innsendt meldekort med id $meldekortId."
        override val kanPrøveIgjen: Boolean = true
        override val loggnivå: Loggnivå = Loggnivå.ERROR
    }
}

/** Nivå [KunneIkkeLagreMeldekortFraBruker.loggMelding] skal logges på i vanlig logg. */
enum class Loggnivå {
    WARN,
    ERROR,
}
