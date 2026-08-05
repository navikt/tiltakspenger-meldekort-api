package no.nav.tiltakspenger

import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import java.util.concurrent.CopyOnWriteArrayList

/**
 * En [Sikkerlogg] som samler opp linjene i minnet i stedet for å skrive dem ut, slik at tester kan asserte på det som havner i sikkerloggen.
 *
 * Samme begrunnelse som [Loggfanger]: per instans, ingen delt tilstand, tåler at vilkårlig mange tester kjører samtidig.
 * Sikkerlogg er et interface i libs nettopp for å kunne injiseres — companion-objektet er bare default-instansen for kallsteder som ennå ikke gjør det.
 */
class Sikkerloggfanger(
    override val seSikkerlogg: String = "Se sikkerlogg.",
) : Sikkerlogg {

    enum class Nivå { TRACE, DEBUG, INFO, WARN, ERROR }

    data class Sikkerlogglinje(
        val nivå: Nivå,
        val melding: String?,
        val feil: Throwable?,
    ) {
        /** Meldingene i hele årsakskjeden, ytterst først. */
        fun årsakskjede(): String =
            generateSequence(feil) { it.cause }.mapNotNull { it.message }.joinToString(" | ")
    }

    private val linjer = CopyOnWriteArrayList<Sikkerlogglinje>()

    val sikkerlogglinjer: List<Sikkerlogglinje> get() = linjer.toList()

    fun linjerPå(nivå: Nivå): List<Sikkerlogglinje> = sikkerlogglinjer.filter { it.nivå == nivå }

    private fun registrer(nivå: Nivå, throwable: Throwable?, loggstatement: () -> Any?) {
        linjer.add(Sikkerlogglinje(nivå = nivå, melding = loggstatement()?.toString(), feil = throwable))
    }

    override fun trace(throwable: Throwable?, loggstatement: () -> Any?) = registrer(Nivå.TRACE, throwable, loggstatement)

    override fun debug(throwable: Throwable?, loggstatement: () -> Any?) = registrer(Nivå.DEBUG, throwable, loggstatement)

    override fun info(throwable: Throwable?, loggstatement: () -> Any?) = registrer(Nivå.INFO, throwable, loggstatement)

    override fun warn(throwable: Throwable?, loggstatement: () -> Any?) = registrer(Nivå.WARN, throwable, loggstatement)

    override fun error(throwable: Throwable?, loggstatement: () -> Any?) = registrer(Nivå.ERROR, throwable, loggstatement)
}
