package no.nav.tiltakspenger.meldekort.meldekort

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import java.time.Clock

/**
 * Alle meldekortene for en meldeperiodekjede.
 * Sortert først på versjon og deretter innsendingstidspunkt.
 */
data class MeldekortForKjede(
    private val meldekort: List<BrukersMeldekort>,
) : List<BrukersMeldekort> by meldekort {
    /**
     * Null dersom listen er tom
     */
    val kjedeId = meldekort.firstOrNull()?.meldeperiode?.kjedeId

    val harInnsendtMeldekort by lazy { meldekort.any { it.erInnsendt } }

    fun erSisteMeldekortKlarTilInnsending(clock: Clock): Boolean =
        meldekort.lastOrNull()?.klarTilInnsending(clock) == true

    fun sisteInnsendteMeldekort(): BrukersMeldekort? = meldekort.lastOrNull { it.erInnsendt }

    fun kanMeldekortKorrigeres(meldekortId: MeldekortId): Boolean =
        harInnsendtMeldekort && sisteInnsendteMeldekort()?.id == meldekortId

    fun korriger(
        command: KorrigerMeldekortCommand,
        sisteMeldeperiode: Meldeperiode,
        clock: Clock,
    ): Either<FeilVedKorrigeringAvMeldekort, BrukersMeldekort> {
        require(this.harInnsendtMeldekort) {
            "Finner ingen innsendinger for valgt periode. Dette skulle vært en førstegangsinnsending, ikke en korrigering. MeldekortId: ${command.meldekortId}. Periode: ${sisteMeldeperiode.periode}. SakId: ${sisteMeldeperiode.sakId}. Saksnummer: ${sisteMeldeperiode.saksnummer}."
        }

        require(command.meldekortId == sisteInnsendteMeldekort()!!.id) {
            return FeilVedKorrigeringAvMeldekort.IkkeSisteMeldekortIKjeden.left()
        }

        return byggKorrigertMeldekort(command, sisteMeldeperiode, clock).right()
    }

    /**
     * Bygger det korrigerte meldekortet uten å validere hvilken tilstand kjeden er i.
     * Gjenbrukes av [korriger] og av korrigering som tar høyde for meldekortvedtak
     * ([no.nav.tiltakspenger.meldekort.meldekort.RegistrertMeldekortForKjede]).
     *
     * Dersom siste meldekort i kjeden er klart til innsending (typisk et åpent/uinnsendt meldekort),
     * fylles dette ut. Ellers opprettes et nytt meldekort for [sisteMeldeperiode].
     */
    fun byggKorrigertMeldekort(
        command: KorrigerMeldekortCommand,
        sisteMeldeperiode: Meldeperiode,
        clock: Clock,
    ): BrukersMeldekort {
        return if (erSisteMeldekortKlarTilInnsending(clock)) {
            meldekort.last().fyllUtMeldekortFraBruker(
                sisteMeldeperiode = sisteMeldeperiode,
                clock = clock,
                brukerutfylteDager = command.korrigerteDager,
                korrigering = true,
                locale = command.locale,
            )
        } else {
            byggNyttKorrigertMeldekort(command, sisteMeldeperiode, clock)
        }
    }

    /**
     * Bygger alltid et nytt (mottatt) korrigert [BrukersMeldekort] for [sisteMeldeperiode],
     * uavhengig av tilstanden til kjedens åpne meldekort.
     *
     * Brukes for korrigering av papir-only kjeder (meldekortvedtak uten digital innsending), der
     * kjedens åpne placeholder-meldekort aldri skal fylles ut in-place, men i stedet deaktiveres.
     */
    fun byggNyttKorrigertMeldekort(
        command: KorrigerMeldekortCommand,
        sisteMeldeperiode: Meldeperiode,
        clock: Clock,
    ): BrukersMeldekort {
        return BrukersMeldekort(
            id = MeldekortId.random(),
            deaktivert = null,
            mottatt = nå(clock),
            meldeperiode = sisteMeldeperiode,
            dager = command.korrigerteDager,
            journalpostId = null,
            journalføringstidspunkt = null,
            korrigering = true,
            locale = command.locale,
        )
    }

    init {
        meldekort.groupBy { it.meldeperiode.versjon }.values.forEach {
            require(it.count { it.mottatt == null } <= 1) {
                "Det kan ikke være mer enn ett meldekort som ikke er mottatt for hver versjon av meldeperioden. Dette skjedde for meldeperioden med kjedeId ${it.first().meldeperiode.kjedeId} og versjon ${it.first().meldeperiode.versjon}."
            }
        }

        meldekort.zipWithNext { a, b ->
            require(a.meldeperiode.versjon <= b.meldeperiode.versjon) {
                """Meldekortene må være sortert på versjon. Feil i kjedeId ${a.meldeperiode.kjedeId}.
                        a: ${a.meldeperiode.versjon}.
                        b: ${b.meldeperiode.versjon}.
                """.trimIndent()
            }

            // Vi er kun interessert å sammenligne mottatt innenfor samme versjon av meldeperioden
            if (a.meldeperiode.versjon == b.meldeperiode.versjon) {
                if (a.erInnsendt && b.erInnsendt) {
                    require(a.mottatt!! <= b.mottatt) {
                        """
                        Meldekortene må være sortert på mottatt. Feil i kjedeId ${a.meldeperiode.kjedeId}.
                            a: ${a.mottatt}.
                            b: ${b.mottatt}.
                        """.trimIndent()
                    }
                } else if (a.mottatt == null && b.mottatt == null) {
                    /*
                     * Hvis begge er null innenfor samme versjon
                     *  For korrigering - så vil man ha samme meldeperiode-versjon, men mottatt vil alltid være satt
                     * I utgangspunktet så skal ikke det skje at a og b har mottatt som null.
                     */
                    throw IllegalArgumentException("Meldekortene kan ikke ha mottatt som null innenfor samme versjon av meldeperioden. Feil i kjedeId ${a.meldeperiode.kjedeId}, for meldekort a ${a.id}, og b ${b.id}")
                }
            }
        }
    }
}
