package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.meldekort.domene.VarselId
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Et varsel som skal sendes til en bruker i forbindelse med utfylling av et eller flere meldekort.
 * Vi oppretter kun et [Varsel] dersom vi vet at det skal sendes et varsel nå eller i fremtiden.
 * Varselet har et tidspunkt for når det skal aktiveres, og eventuelt et tidspunkt for når det skal inaktiveres.
 * Tilstandsmaskin:
 * SkalAktiveres ──aktiver()──→ Aktiv ──forberedInaktivering()──→ SkalInaktiveres ──inaktiver()──→ Inaktivert
 *      │
 *      └──avbryt()──→ Avbrutt
 *
 * Begrunnelses-feltene ([skalAktiveresBegrunnelse], [skalInaktiveresBegrunnelse], [avbruttBegrunnelse])
 * er tekniske grunner og skal ikke inneholde personopplysninger.
 */
sealed interface Varsel {
    val sakId: SakId
    val saksnummer: String
    val fnr: Fnr
    val varselId: VarselId
    val skalAktiveresTidspunkt: LocalDateTime
    val skalAktiveresBegrunnelse: String
    val aktiveringstidspunkt: LocalDateTime?

    val skalInaktiveresTidspunkt: LocalDateTime?
    val skalInaktiveresBegrunnelse: String?
    val inaktiveringstidspunkt: LocalDateTime?
    val avbruttTidspunkt: LocalDateTime?
    val avbruttBegrunnelse: String?

    val opprettet: LocalDateTime
    val sistEndret: LocalDateTime

    val type: String
        get() = when (this) {
            is SkalAktiveres -> "SkalAktiveres"
            is Aktiv -> "Aktiv"
            is SkalInaktiveres -> "SkalInaktiveres"
            is Inaktivert -> "Inaktivert"
            is Avbrutt -> "Avbrutt"
        }

    val skalAktiveres: Boolean get() = this is SkalAktiveres

    /** Har varselet noen gang vært aktivt? */
    val harVærtAktivt: Boolean get() = this is Aktiv || this is SkalInaktiveres || this is Inaktivert

    /** Er varselet aktivt nå? (det er aktivt frem til det er [Inaktivert]) */
    val erAktivt: Boolean get() = this is Aktiv || this is SkalInaktiveres

    val skalInaktiveres: Boolean get() = this is SkalInaktiveres

    /** Er varselet inaktivert? (krever at det har vært aktivt på et tidspunkt) */
    val erInaktivert: Boolean get() = this is Inaktivert

    val erAvbrutt: Boolean get() = this is Avbrutt

    val erInaktivertEllerAvbrutt: Boolean get() = this is Inaktivert || this is Avbrutt

    /** Ikke aktivt enda, men skal aktiveres på [skalAktiveresTidspunkt]. */
    data class SkalAktiveres(
        override val sakId: SakId,
        override val saksnummer: String,
        override val fnr: Fnr,
        override val varselId: VarselId,
        override val skalAktiveresTidspunkt: LocalDateTime,
        override val skalAktiveresBegrunnelse: String,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
    ) : Varsel {

        override val aktiveringstidspunkt = null
        override val skalInaktiveresTidspunkt = null
        override val skalInaktiveresBegrunnelse = null
        override val inaktiveringstidspunkt = null
        override val avbruttBegrunnelse = null
        override val avbruttTidspunkt = null

        init {
            require(skalAktiveresTidspunkt.hour in 9..16) {
                "Varsel.SkalAktiveres: skalAktiveresTidspunkt $skalAktiveresTidspunkt er utenfor tillatt tidsrom (09:00-17:00)"
            }
            require(skalAktiveresTidspunkt.dayOfWeek != DayOfWeek.SATURDAY && skalAktiveresTidspunkt.dayOfWeek != DayOfWeek.SUNDAY) {
                "Varsel.SkalAktiveres: skalAktiveresTidspunkt $skalAktiveresTidspunkt er på en helgedag"
            }
        }

        /**
         * Aktiverer varselet.
         *
         * @return [Aktiv] dersom [aktiveringstidspunkt] er lik eller etter [skalAktiveresTidspunkt],
         *         ellers [KanIkkeAktivere.ForTidlig].
         */
        fun aktiver(
            aktiveringstidspunkt: LocalDateTime,
        ): Either<KanIkkeAktivere, Aktiv> {
            if (aktiveringstidspunkt < skalAktiveresTidspunkt) {
                return KanIkkeAktivere.ForTidlig(skalAktiveresTidspunkt, aktiveringstidspunkt).left()
            }
            return Aktiv(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                varselId = varselId,
                skalAktiveresTidspunkt = skalAktiveresTidspunkt,
                skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
                aktiveringstidspunkt = aktiveringstidspunkt,
                opprettet = opprettet,
                sistEndret = aktiveringstidspunkt,
            ).right()
        }

        /**
         * Avbryter varselet før det er aktivert.
         * Brukes når bruker har gått fra rett til ingen rett (total stans/opphør).
         * Dersom varselet allerede er aktivert, skal man bruke [Aktiv.forberedInaktivering] og [SkalInaktiveres.inaktiver] i stedet.
         */
        fun avbryt(
            avbruttTidspunkt: LocalDateTime,
            avbruttBegrunnelse: String,
        ): Avbrutt {
            return Avbrutt(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                varselId = varselId,
                skalAktiveresTidspunkt = skalAktiveresTidspunkt,
                skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
                avbruttTidspunkt = avbruttTidspunkt,
                avbruttBegrunnelse = avbruttBegrunnelse,
                opprettet = opprettet,
                sistEndret = avbruttTidspunkt,
            )
        }

        /**
         * I tilfeller der en meldeperiode frem i tid ikke lenger skal fylles ut (delvis opphør), men bruker har senere meldeperioder i fremtiden som skal fylles ut.
         */
        fun planleggPåNytt(
            skalAktiveresTidspunkt: LocalDateTime,
            skalAktiveresBegrunnelse: String,
            sistEndret: LocalDateTime,
        ): SkalAktiveres {
            require(skalAktiveres)
            return copy(
                skalAktiveresTidspunkt = skalAktiveresTidspunkt,
                skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
                sistEndret = sistEndret,
            )
        }

        override fun toString(): String {
            return "Varsel.SkalAktiveres(varselId=$varselId, sakId=$sakId, saksnummer='$saksnummer', fnr=*****, skalAktiveresTidspunkt=$skalAktiveresTidspunkt, skalAktiveresBegrunnelse=$skalAktiveresBegrunnelse)"
        }
    }

    /** Varselet er aktivt og synlig for bruker. Kan forberede inaktivering via [forberedInaktivering].*/
    data class Aktiv(
        override val sakId: SakId,
        override val saksnummer: String,
        override val fnr: Fnr,
        override val varselId: VarselId,
        override val skalAktiveresTidspunkt: LocalDateTime,
        override val skalAktiveresBegrunnelse: String,
        override val aktiveringstidspunkt: LocalDateTime,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
    ) : Varsel {

        override val skalInaktiveresTidspunkt = null
        override val skalInaktiveresBegrunnelse = null
        override val inaktiveringstidspunkt = null
        override val avbruttBegrunnelse = null
        override val avbruttTidspunkt = null

        init {
            require(aktiveringstidspunkt >= skalAktiveresTidspunkt) {
                "Varsel.Aktiv: aktiveringstidspunkt $aktiveringstidspunkt må være lik eller etter skalAktiveresTidspunkt $skalAktiveresTidspunkt"
            }
        }

        /**
         * Forbereder en fremtidig inaktivering av varselet.
         * Selve inaktiveringen skjer ved å kalle [SkalInaktiveres.inaktiver] på eller etter [skalInaktiveresTidspunkt].
         *
         * Merk: [skalInaktiveresTidspunkt] må være strengt etter [aktiveringstidspunkt] (ikke lik),
         * siden varselet må ha vært synlig i minst et øyeblikk.
         *
         * @return [SkalInaktiveres] dersom [skalInaktiveresTidspunkt] er strengt etter [aktiveringstidspunkt],
         *         ellers [KanIkkeForberedeInaktivering.ForTidlig].
         */
        fun forberedInaktivering(
            skalInaktiveresTidspunkt: LocalDateTime,
            skalInaktiveresBegrunnelse: String,
        ): Either<KanIkkeForberedeInaktivering, SkalInaktiveres> {
            if (skalInaktiveresTidspunkt <= aktiveringstidspunkt) {
                return KanIkkeForberedeInaktivering.ForTidlig(aktiveringstidspunkt, skalInaktiveresTidspunkt).left()
            }
            return SkalInaktiveres(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                varselId = varselId,
                skalAktiveresTidspunkt = skalAktiveresTidspunkt,
                skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
                aktiveringstidspunkt = aktiveringstidspunkt,
                skalInaktiveresTidspunkt = skalInaktiveresTidspunkt,
                skalInaktiveresBegrunnelse = skalInaktiveresBegrunnelse,
                opprettet = opprettet,
                sistEndret = skalInaktiveresTidspunkt,
            ).right()
        }

        override fun toString(): String {
            return "Varsel.Aktiv(varselId=$varselId, sakId=$sakId, saksnummer='$saksnummer', fnr=*****, skalAktiveresTidspunkt=$skalAktiveresTidspunkt, skalAktiveresBegrunnelse=$skalAktiveresBegrunnelse, aktiveringstidspunkt=$aktiveringstidspunkt)"
        }
    }

    /** Varselet er fremdeles aktivt, men skal inaktiveres på [skalInaktiveresTidspunkt].*/
    data class SkalInaktiveres(
        override val sakId: SakId,
        override val saksnummer: String,
        override val fnr: Fnr,
        override val varselId: VarselId,
        override val skalAktiveresTidspunkt: LocalDateTime,
        override val skalAktiveresBegrunnelse: String,
        override val aktiveringstidspunkt: LocalDateTime,
        override val skalInaktiveresTidspunkt: LocalDateTime,
        override val skalInaktiveresBegrunnelse: String,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
    ) : Varsel {

        override val inaktiveringstidspunkt = null
        override val avbruttBegrunnelse = null
        override val avbruttTidspunkt = null

        init {
            require(aktiveringstidspunkt >= skalAktiveresTidspunkt) {
                "Varsel.SkalInaktiveres: aktiveringstidspunkt $aktiveringstidspunkt må være lik eller etter skalAktiveresTidspunkt $skalAktiveresTidspunkt"
            }
            require(skalInaktiveresTidspunkt > aktiveringstidspunkt) {
                "Varsel.SkalInaktiveres: skalInaktiveresTidspunkt $skalInaktiveresTidspunkt må være etter aktiveringstidspunkt $aktiveringstidspunkt"
            }
        }

        /**
         * Inaktiverer varselet.
         *
         * @return [Inaktivert] dersom [inaktiveringstidspunkt] er lik eller etter [skalInaktiveresTidspunkt],
         *         ellers [KanIkkeInaktivere.ForTidlig].
         */
        fun inaktiver(
            inaktiveringstidspunkt: LocalDateTime,
        ): Either<KanIkkeInaktivere, Inaktivert> {
            if (inaktiveringstidspunkt < skalInaktiveresTidspunkt) {
                return KanIkkeInaktivere.ForTidlig(skalInaktiveresTidspunkt, inaktiveringstidspunkt).left()
            }
            return Inaktivert(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                varselId = varselId,
                skalAktiveresTidspunkt = skalAktiveresTidspunkt,
                skalAktiveresBegrunnelse = skalAktiveresBegrunnelse,
                aktiveringstidspunkt = aktiveringstidspunkt,
                skalInaktiveresTidspunkt = skalInaktiveresTidspunkt,
                skalInaktiveresBegrunnelse = skalInaktiveresBegrunnelse,
                inaktiveringstidspunkt = inaktiveringstidspunkt,
                opprettet = opprettet,
                sistEndret = inaktiveringstidspunkt,
            ).right()
        }

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
        override val skalAktiveresBegrunnelse: String,
        override val aktiveringstidspunkt: LocalDateTime,
        override val skalInaktiveresTidspunkt: LocalDateTime,
        override val skalInaktiveresBegrunnelse: String,
        override val inaktiveringstidspunkt: LocalDateTime,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
    ) : Varsel {

        override val avbruttBegrunnelse = null
        override val avbruttTidspunkt = null

        init {
            require(aktiveringstidspunkt >= skalAktiveresTidspunkt) {
                "Varsel.Inaktivert: aktiveringstidspunkt $aktiveringstidspunkt må være lik eller etter skalAktiveresTidspunkt $skalAktiveresTidspunkt"
            }
            require(skalInaktiveresTidspunkt > aktiveringstidspunkt) {
                "Varsel.Inaktivert: skalInaktiveresTidspunkt $skalInaktiveresTidspunkt må være etter aktiveringstidspunkt $aktiveringstidspunkt"
            }
            require(inaktiveringstidspunkt >= skalInaktiveresTidspunkt) {
                "Varsel.Inaktivert: inaktiveringstidspunkt $inaktiveringstidspunkt må være lik eller etter skalInaktiveresTidspunkt $skalInaktiveresTidspunkt"
            }
        }

        override fun toString(): String =
            "Varsel.Inaktivert(varselId=$varselId, sakId=$sakId, saksnummer='$saksnummer', fnr=*****, skalAktiveresTidspunkt=$skalAktiveresTidspunkt, skalAktiveresBegrunnelse=$skalAktiveresBegrunnelse, aktiveringstidspunkt=$aktiveringstidspunkt, skalInaktiveresTidspunkt=$skalInaktiveresTidspunkt, skalInaktiveresBegrunnelse=$skalInaktiveresBegrunnelse, inaktiveringstidspunkt=$inaktiveringstidspunkt)"
    }

    /**
     * Varselet ble avbrutt før det ble aktivert (f.eks. ved stans/opphør). Terminal tilstand.
     * @param avbruttTidspunkt tidspunktet varselet ble avbrutt. I noen unntakstilfeller kan dette være etter [skalAktiveresTidspunkt] hvis det har vært forsinkelser.
     */
    data class Avbrutt(
        override val sakId: SakId,
        override val saksnummer: String,
        override val fnr: Fnr,
        override val varselId: VarselId,
        override val skalAktiveresTidspunkt: LocalDateTime,
        override val skalAktiveresBegrunnelse: String,
        override val avbruttTidspunkt: LocalDateTime,
        override val avbruttBegrunnelse: String,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
    ) : Varsel {

        override val aktiveringstidspunkt = null
        override val skalInaktiveresTidspunkt = null
        override val skalInaktiveresBegrunnelse = null
        override val inaktiveringstidspunkt = null

        override fun toString(): String {
            return "Varsel.Avbrutt(varselId=$varselId, sakId=$sakId, saksnummer='$saksnummer', fnr=*****, skalAktiveresTidspunkt=$skalAktiveresTidspunkt, skalAktiveresBegrunnelse=$skalAktiveresBegrunnelse, avbruttTidspunkt=$avbruttTidspunkt, avbruttBegrunnelse=$avbruttBegrunnelse)"
        }
    }

    sealed interface KanIkkeAktivere {
        val melding: String

        data class ForTidlig(
            val skalAktiveresTidspunkt: LocalDateTime,
            val aktiveringstidspunkt: LocalDateTime,
        ) : KanIkkeAktivere {
            override val melding = "Kan ikke aktivere varsel før $skalAktiveresTidspunkt (forsøkt: $aktiveringstidspunkt)"
        }
    }

    sealed interface KanIkkeForberedeInaktivering {
        val melding: String

        data class ForTidlig(
            val aktiveringstidspunkt: LocalDateTime,
            val skalInaktiveresTidspunkt: LocalDateTime,
        ) : KanIkkeForberedeInaktivering {
            override val melding = "Kan ikke forberede inaktivering med tidspunkt $skalInaktiveresTidspunkt som ikke er etter aktiveringstidspunkt $aktiveringstidspunkt"
        }
    }

    sealed interface KanIkkeInaktivere {
        val melding: String

        data class ForTidlig(
            val skalInaktiveresTidspunkt: LocalDateTime,
            val inaktiveringstidspunkt: LocalDateTime,
        ) : KanIkkeInaktivere {
            override val melding = "Kan ikke inaktivere varsel før $skalInaktiveresTidspunkt (forsøkt: $inaktiveringstidspunkt)"
        }
    }
}
