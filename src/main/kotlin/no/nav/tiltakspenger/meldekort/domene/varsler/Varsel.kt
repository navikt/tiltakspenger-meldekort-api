package no.nav.tiltakspenger.meldekort.domene.varsler

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.meldekort.domene.VarselId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal val VARSEL_ÅPNER: LocalTime = LocalTime.of(9, 0)
internal val VARSEL_STENGER: LocalTime = LocalTime.of(17, 0)

/**
 * Et varsel som skal sendes til en bruker i forbindelse med utfylling av et eller flere meldekort.
 * Vi oppretter kun et [Varsel] dersom vi vet at det skal sendes et varsel nå eller i fremtiden.
 *
 * Tilstandsmaskin:
 * ```
 * SkalAktiveres ──aktiver()──→ Aktiv ──forberedInaktivering()──→ SkalInaktiveres ──inaktiver()──→ Inaktivert
 * Ved avbrytelse: SkalAktiveres ──forberedInaktivering──→ SkalInaktiveres (uten å gå via Aktiv).
 * ```
 *
 * Begrunnelses-feltene ([skalAktiveresBegrunnelse], [skalInaktiveresBegrunnelse]) er tekniske grunner og skal ikke inneholde personopplysninger.
 */
sealed interface Varsel {
    val sakId: SakId
    val saksnummer: String
    val fnr: Fnr
    val varselId: VarselId

    /**
     * Planlagt tidspunkt for aktivering på min side (intern varsling).
     * Merk at dette ikke er det samme som [skalAktiveresEksterntTidspunkt] (ekstern varsling).
     * Vi garanterer at varselet ikke blir produsert på Kafka før dette.
     * Kan være tilbake i tid.
     */
    val skalAktiveresTidspunkt: LocalDateTime

    /**
     * Ekstern varsling (sms/e-post via Altinn).
     * Må være innenfor åpningstid (09:00-17:00) på en virkedag.
     * Brukes som `utsettSendingTil` mot Min side når varselet aktiveres.
     * Grunnen til at vi setter dette er at Min side sender varselet til Altinn og da mister vi muligheten til å kansellere det eksterne varselet.
     */
    val skalAktiveresEksterntTidspunkt: LocalDateTime?

    /**
     * Begrunnelse for hvorfor varselet skal aktiveres. Skal ikke inneholde personopplysninger, kun tekniske grunner for debugging i databasen.
     */
    val skalAktiveresBegrunnelse: String

    /**
     * Faktisk tidspunkt varselet ble aktivert (dvs. produsert på Kafka mot Min side).
     * Null frem til varselet går over i [Aktiv].
     * Kan ikke være før [skalAktiveresTidspunkt].
     * Vil være null dersom vi går direkte fra [SkalAktiveres] til [SkalInaktiveres] uten å gå innom [Aktiv] (f.eks. hvis vi oppdager at varselet ikke skulle vært sendt før det ble aktivert).
     */
    val aktiveringstidspunkt: LocalDateTime?

    /**
     * Planlagt tidspunkt for ekstern varsling (sms/e-post via Altinn).
     * Må være innenfor åpningstid (09:00-17:00) på en virkedag.
     * Settes når varselet går fra [SkalAktiveres] til [Aktiv].
     * Kan ikke være før [aktiveringstidspunkt].
     * Vil være null dersom vi går direkte fra [SkalAktiveres] til [SkalInaktiveres] uten å gå innom [Aktiv] (f.eks. hvis vi oppdager at varselet ikke skulle vært sendt før det ble aktivert).
     */
    val eksternAktiveringstidspunkt: LocalDateTime?

    /** Planlagt tidspunkt for inaktivering. Settes når varselet går fra [SkalAktiveres] eller [Aktiv] til [SkalInaktiveres]. */
    val skalInaktiveresTidspunkt: LocalDateTime?

    /** Begrunnelse for hvorfor varselet skal inaktiveres. Skal ikke inneholde personopplysninger, kun tekniske grunner for debugging i databasen. Settes når varselet går fra [SkalAktiveres] eller [Aktiv] til [SkalInaktiveres]. */
    val skalInaktiveresBegrunnelse: String?

    /** Faktisk tidspunkt varselet ble inaktivert. Kun satt i tilstanden [Inaktivert]. */
    val inaktiveringstidspunkt: LocalDateTime?

    /** Settes kun når vi oppretter varselet. */
    val opprettet: LocalDateTime

    /** Oppdateres ved hver tilstandsovergang. */
    val sistEndret: LocalDateTime

    /** True hvis vi er i tilstanden [Inaktivert]. */
    val erInaktivert: Boolean get() = this is Inaktivert

    /** True hvis varselet faktisk ble aktivert på [dato]. */
    fun erAktivertPå(dato: LocalDate): Boolean = aktiveringstidspunkt?.toLocalDate() == dato

    /** Ikke produsert på Kafka enda. Min side får hendelsen først når varselet går over i [Aktiv]. */
    data class SkalAktiveres(
        override val sakId: SakId,
        override val saksnummer: String,
        override val fnr: Fnr,
        override val varselId: VarselId,
        override val skalAktiveresTidspunkt: LocalDateTime,
        override val skalAktiveresEksterntTidspunkt: LocalDateTime,
        override val skalAktiveresBegrunnelse: String,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
    ) : Varsel {

        override val aktiveringstidspunkt = null
        override val eksternAktiveringstidspunkt = null
        override val skalInaktiveresTidspunkt = null
        override val skalInaktiveresBegrunnelse = null
        override val inaktiveringstidspunkt = null

        init {
            if (skalAktiveresEksterntTidspunkt.isBefore(skalAktiveresTidspunkt)) {
                throw IllegalArgumentException("Varsel.SkalAktiveres: skalAktiveresEksterntTidspunkt $skalAktiveresEksterntTidspunkt kan ikke være før skalAktiveresTidspunkt $skalAktiveresTidspunkt")
            }
            require(
                !skalAktiveresEksterntTidspunkt.toLocalTime().isBefore(VARSEL_ÅPNER) &&
                    skalAktiveresEksterntTidspunkt.toLocalTime().isBefore(VARSEL_STENGER),
            ) {
                "Varsel.SkalAktiveres: skalAktiveresEksterntTidspunkt $skalAktiveresEksterntTidspunkt er utenfor tillatt tidsrom (09:00-17:00)"
            }
            require(!skalAktiveresEksterntTidspunkt.toLocalDate().erHelg()) {
                "Varsel.SkalAktiveres: skalAktiveresEksterntTidspunkt $skalAktiveresEksterntTidspunkt er på en helgedag"
            }
        }

        /**
         * Aktiverer varselet.
         *
         * Produksjonskode skal gå via [Varsler.aktiver] slik at hele aggregatet valideres før persistering.
         * Denne funksjonen er ment som en ren tilstandsovergang på enkeltobjektet.
         *
         * @param eksternAktiveringstidspunkt må være innenfor åpningstid (09:00-17:00) på en virkedag, og kan ikke være før [aktiveringstidspunkt].
         */
        fun aktiver(
            aktiveringstidspunkt: LocalDateTime,
        ): Aktiv = Aktiv(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
            skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
            aktiveringstidspunkt = aktiveringstidspunkt,
            eksternAktiveringstidspunkt = maxOf(
                skalAktiveresEksterntTidspunkt,
                aktiveringstidspunkt,
            ).nesteGyldigeEksternVarseltidspunkt(),
            opprettet = opprettet,
            sistEndret = aktiveringstidspunkt,
        )

        /**
         * Markerer varselet for inaktivering.
         * [skalInaktiveresTidspunkt] kan være lik eller etter [aktiveringstidspunkt].
         * Vi kan inaktivere umiddelbart etter aktivering dersom vi oppdager at varselet ikke skulle vært sendt.
         *
         * Produksjonskode skal gå via [Varsler.forberedInaktivering] slik at hele aggregatet valideres før persistering.
         * Denne funksjonen er ment som en ren tilstandsovergang på enkeltobjektet.
         */
        fun forberedInaktivering(
            skalInaktiveresTidspunkt: LocalDateTime,
            skalInaktiveresBegrunnelse: String,
        ): SkalInaktiveres = SkalInaktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
            skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
            aktiveringstidspunkt = aktiveringstidspunkt,
            eksternAktiveringstidspunkt = eksternAktiveringstidspunkt,
            skalInaktiveresTidspunkt = skalInaktiveresTidspunkt,
            skalInaktiveresBegrunnelse = skalInaktiveresBegrunnelse,
            opprettet = opprettet,
            sistEndret = skalInaktiveresTidspunkt,
        )

        override fun toString(): String {
            return "Varsel.SkalAktiveres(varselId=$varselId, sakId=$sakId, saksnummer='$saksnummer', fnr=*****, skalAktiveresTidspunkt=$skalAktiveresTidspunkt, skalAktiveresBegrunnelse=$skalAktiveresBegrunnelse)"
        }
    }

    /** Varselet er produsert på Kafka mot Min side. Kan forberede inaktivering via [forberedInaktivering]. */
    data class Aktiv(
        override val sakId: SakId,
        override val saksnummer: String,
        override val fnr: Fnr,
        override val varselId: VarselId,
        override val skalAktiveresTidspunkt: LocalDateTime,
        override val skalAktiveresEksterntTidspunkt: LocalDateTime,
        override val skalAktiveresBegrunnelse: String,
        override val aktiveringstidspunkt: LocalDateTime,
        override val eksternAktiveringstidspunkt: LocalDateTime,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
    ) : Varsel {

        override val skalInaktiveresTidspunkt = null
        override val skalInaktiveresBegrunnelse = null
        override val inaktiveringstidspunkt = null

        init {
            // Sikkerhetsnett for ikke å sende varsel til Min side før planlagt tidspunkt, og for å sikre at ekstern varslingstidspunkt ikke er før aktiveringstidspunkt.
            require(skalAktiveresEksterntTidspunkt >= skalAktiveresTidspunkt)
            require(aktiveringstidspunkt >= skalAktiveresTidspunkt)
            require(eksternAktiveringstidspunkt >= aktiveringstidspunkt)
        }

        /**
         * Markerer varselet for inaktivering. [skalInaktiveresTidspunkt] kan være lik eller etter
         * [aktiveringstidspunkt] – vi kan inaktivere umiddelbart etter aktivering dersom vi
         * oppdager at varselet ikke skulle vært sendt.
         *
         * Produksjonskode skal gå via [Varsler.forberedInaktivering] slik at hele aggregatet
         * valideres før persistering. Denne funksjonen er ment som en ren tilstandsovergang på
         * enkeltobjektet.
         */
        fun forberedInaktivering(
            skalInaktiveresTidspunkt: LocalDateTime,
            skalInaktiveresBegrunnelse: String,
        ): SkalInaktiveres = SkalInaktiveres(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
            skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
            aktiveringstidspunkt = aktiveringstidspunkt,
            eksternAktiveringstidspunkt = eksternAktiveringstidspunkt,
            skalInaktiveresTidspunkt = skalInaktiveresTidspunkt,
            skalInaktiveresBegrunnelse = skalInaktiveresBegrunnelse,
            opprettet = opprettet,
            sistEndret = skalInaktiveresTidspunkt,
        )

        override fun toString(): String {
            return "Varsel.Aktiv(varselId=$varselId, sakId=$sakId, saksnummer='$saksnummer', fnr=*****, skalAktiveresTidspunkt=$skalAktiveresTidspunkt, skalAktiveresBegrunnelse=$skalAktiveresBegrunnelse, aktiveringstidspunkt=$aktiveringstidspunkt)"
        }
    }

    /**
     * Varselet er fremdeles aktivt, men skal inaktiveres på [skalInaktiveresTidspunkt].
     */
    data class SkalInaktiveres(
        override val sakId: SakId,
        override val saksnummer: String,
        override val fnr: Fnr,
        override val varselId: VarselId,
        override val skalAktiveresTidspunkt: LocalDateTime,
        override val skalAktiveresEksterntTidspunkt: LocalDateTime,
        override val skalAktiveresBegrunnelse: String,
        override val aktiveringstidspunkt: LocalDateTime?,
        override val eksternAktiveringstidspunkt: LocalDateTime?,
        override val skalInaktiveresTidspunkt: LocalDateTime,
        override val skalInaktiveresBegrunnelse: String,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
    ) : Varsel {

        override val inaktiveringstidspunkt = null

        /**
         * Inaktiverer varselet. [inaktiveringstidspunkt] kan være lik eller etter
         * [skalInaktiveresTidspunkt].
         *
         * Produksjonskode skal gå via [Varsler.inaktiver] slik at hele aggregatet valideres før
         * persistering. Denne funksjonen er ment som en ren tilstandsovergang på enkeltobjektet.
         */
        fun inaktiver(inaktiveringstidspunkt: LocalDateTime): Inaktivert = Inaktivert(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = varselId,
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
            skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
            aktiveringstidspunkt = aktiveringstidspunkt,
            eksternAktiveringstidspunkt = eksternAktiveringstidspunkt,
            skalInaktiveresTidspunkt = skalInaktiveresTidspunkt,
            skalInaktiveresBegrunnelse = skalInaktiveresBegrunnelse,
            inaktiveringstidspunkt = inaktiveringstidspunkt,
            opprettet = opprettet,
            sistEndret = inaktiveringstidspunkt,
        )

        override fun toString(): String =
            "Varsel.SkalInaktiveres(varselId=$varselId, sakId=$sakId, saksnummer='$saksnummer', fnr=*****, skalAktiveresTidspunkt=$skalAktiveresTidspunkt, skalAktiveresBegrunnelse=$skalAktiveresBegrunnelse, aktiveringstidspunkt=$aktiveringstidspunkt, skalInaktiveresTidspunkt=$skalInaktiveresTidspunkt, skalInaktiveresBegrunnelse=$skalInaktiveresBegrunnelse)"
    }

    /** Varselet har tidligere vært aktivt, men er nå inaktivert. Terminal tilstand. */
    data class Inaktivert(
        override val sakId: SakId,
        override val saksnummer: String,
        override val fnr: Fnr,
        override val varselId: VarselId,
        override val skalAktiveresTidspunkt: LocalDateTime,
        override val skalAktiveresEksterntTidspunkt: LocalDateTime,
        override val skalAktiveresBegrunnelse: String,
        override val aktiveringstidspunkt: LocalDateTime?,
        override val eksternAktiveringstidspunkt: LocalDateTime?,
        override val skalInaktiveresTidspunkt: LocalDateTime,
        override val skalInaktiveresBegrunnelse: String,
        override val inaktiveringstidspunkt: LocalDateTime,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
    ) : Varsel {

        override fun toString(): String =
            "Varsel.Inaktivert(varselId=$varselId, sakId=$sakId, saksnummer='$saksnummer', fnr=*****, skalAktiveresTidspunkt=$skalAktiveresTidspunkt, skalAktiveresBegrunnelse=$skalAktiveresBegrunnelse, aktiveringstidspunkt=$aktiveringstidspunkt, skalInaktiveresTidspunkt=$skalInaktiveresTidspunkt, skalInaktiveresBegrunnelse=$skalInaktiveresBegrunnelse, inaktiveringstidspunkt=$inaktiveringstidspunkt)"
    }
}

/** Er det aktivert et varsel i denne samlingen på [dato]? */
fun List<Varsel>.harAktivertVarselSammeDag(dato: LocalDate): Boolean = any { it.erAktivertPå(dato) }

/**
 * Finner det planlagte aktiveringstidspunktet for et nytt varsel, justert til:
 *  - neste gyldige varseltidspunkt (virkedag 09:00-17:00)
 *  - neste virkedag dersom det allerede er aktivert et varsel samme dag (cooldown)
 *
 *  Merk at denne ikke tar høyde for helligdager.
 */
fun List<Varsel>.finnSkalAktiveresEksterntTidspunkt(
    ønsketTidspunkt: LocalDateTime,
    nå: LocalDateTime,
): LocalDateTime {
    val planlagtTidspunkt = maxOf(ønsketTidspunkt, nå).nesteGyldigeEksternVarseltidspunkt()

    return if (harAktivertVarselSammeDag(nå.toLocalDate()) && planlagtTidspunkt.toLocalDate() == nå.toLocalDate()) {
        nå.toLocalDate().nesteVirkedagKlNi()
    } else {
        planlagtTidspunkt
    }
}

/**
 * Returnerer det neste tidspunktet som er innenfor det eksterne varselvinduet (virkedag 09:00-17:00).
 * Dersom [this] allerede er innenfor vinduet, returneres [this].
 */
internal fun LocalDateTime.nesteGyldigeEksternVarseltidspunkt(): LocalDateTime {
    val dato = toLocalDate()
    val klokkeslett = toLocalTime()

    return when {
        dato.erHelg() -> dato.nesteVirkedagKlNi()
        klokkeslett.isBefore(VARSEL_ÅPNER) -> dato.atTime(VARSEL_ÅPNER)
        !klokkeslett.isBefore(VARSEL_STENGER) -> dato.nesteVirkedagKlNi()
        else -> this
    }
}

/** Neste virkedag etter [this] kl. 09:00. */
internal fun LocalDate.nesteVirkedagKlNi(): LocalDateTime {
    return generateSequence(plusDays(1)) { it.plusDays(1) }
        .first { !it.erHelg() }
        .atTime(VARSEL_ÅPNER)
}

private fun LocalDate.erHelg(): Boolean {
    return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
}
