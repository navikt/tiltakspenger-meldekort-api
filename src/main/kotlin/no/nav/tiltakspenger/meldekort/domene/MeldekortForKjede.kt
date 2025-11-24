package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.service.KorrigerMeldekortCommand
import java.time.Clock
import java.time.LocalDateTime

/**
 * Alle meldekortene for en meldeperiodekjede.
 */
data class MeldekortForKjede(
    private val meldekort: List<Meldekort>,
) : List<Meldekort> by meldekort {
    /**
     * Null dersom listen er tom
     */
    val kjedeId = meldekort.firstOrNull()?.meldeperiode?.kjedeId

    val harInnsendtMeldekort by lazy { meldekort.any { it.erInnsendt } }

    fun erSisteMeldekortKlarTilInnsending(clock: Clock): Boolean =
        meldekort.lastOrNull()?.klarTilInnsending(clock) == true

    fun sisteInnsendteMeldekort(): Meldekort? = meldekort.lastOrNull { it.erInnsendt }

    fun korriger(
        command: KorrigerMeldekortCommand,
        sisteMeldeperiode: Meldeperiode,
        clock: Clock,
    ): Meldekort {
        require(this.harInnsendtMeldekort) {
            "Finner ingen innsendinger for valgt periode. Dette skulle vært en førstegangsinnsending, ikke en korrigering. MeldekortId: ${command.meldekortId}. Periode: ${sisteMeldeperiode.periode}. SakId: ${sisteMeldeperiode.sakId}. Saksnummer: ${sisteMeldeperiode.saksnummer}."
        }

        require(command.meldekortId == sisteInnsendteMeldekort()!!.id) {
            "Meldekort med id ${command.meldekortId} er ikke siste meldekort i kjeden ${sisteInnsendteMeldekort()!!.meldeperiode.kjedeId}. Kan ikke korrigere."
        }

        return if (erSisteMeldekortKlarTilInnsending(clock)) {
            meldekort.last().fyllUtMeldekortFraBruker(
                sisteMeldeperiode = sisteMeldeperiode,
                clock = clock,
                brukerutfylteDager = command.korrigerteDager,
                korrigering = true,
            )
        } else {
            Meldekort(
                id = MeldekortId.random(),
                deaktivert = null,
                mottatt = LocalDateTime.now(clock),
                meldeperiode = sisteMeldeperiode,
                dager = command.korrigerteDager,
                journalpostId = null,
                journalføringstidspunkt = null,
                varselId = null,
                erVarselInaktivert = false,
                korrigering = true,
            )
        }
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
