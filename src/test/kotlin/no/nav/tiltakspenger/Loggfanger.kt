package no.nav.tiltakspenger

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KLoggingEventBuilder
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker
import java.util.concurrent.CopyOnWriteArrayList

/**
 * En [KLogger] som samler opp logglinjene i minnet i stedet for å skrive dem ut, slik at tester kan asserte på innholdet i det som logges.
 *
 * Fangeren er bevisst per instans og deler ingen tilstand: den henger ikke på den globale logback-loggeren, slik en `ListAppender` gjør.
 * Tusenvis av tester kan derfor kjøre samtidig – også innenfor samme testfil – uten at logglinjer lekker mellom dem.
 *
 * Alle bekvemmelighetsmetodene på [KLogger] (`info {}`, `error(t) {}`, `atWarn {}` …) har default-implementasjoner som trakter ned i [at], så det holder å implementere de tre abstrakte medlemmene for å fange enhver kallform.
 */
class Loggfanger(
    override val name: String,
) : KLogger {

    data class Logglinje(
        val nivå: Level,
        val melding: String?,
        val feil: Throwable?,
    ) {
        /**
         * Meldingene i hele årsakskjeden, ytterst først.
         * Feilen som logges er typisk pakket inn flere ganger (f.eks. `RuntimeException` rundt en `PSQLException`), og det er den innerste som bærer detaljene.
         */
        fun årsakskjede(): String =
            generateSequence(feil) { it.cause }.mapNotNull { it.message }.joinToString(" | ")
    }

    private val linjer = CopyOnWriteArrayList<Logglinje>()

    val logglinjer: List<Logglinje> get() = linjer.toList()

    fun linjerPå(nivå: Level): List<Logglinje> = logglinjer.filter { it.nivå == nivå }

    override fun isLoggingEnabledFor(level: Level, marker: Marker?): Boolean = true

    override fun at(level: Level, marker: Marker?, block: KLoggingEventBuilder.() -> Unit) {
        val hendelse = KLoggingEventBuilder().apply(block)
        linjer.add(Logglinje(nivå = level, melding = hendelse.message, feil = hendelse.cause))
    }
}
