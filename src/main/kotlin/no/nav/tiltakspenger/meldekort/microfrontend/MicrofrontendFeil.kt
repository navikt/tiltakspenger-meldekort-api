package no.nav.tiltakspenger.meldekort.microfrontend

/**
 * Feil som kan oppstå i microfrontend-pakken.
 * De lavere lagene (repo og klient) returnerer disse som venstre-side i en [arrow.core.Either] i stedet for å kaste, slik at ytterste lag (en jobb eller en route) kan ta stilling til hva som bør gjøres.
 *
 * Feilen bærer med seg [throwable]-årsaken slik at ytterste lag kan logge én oppsummerende feillinje med
 * full stacktrace, uten at de lavere lagene spammer loggen med én linje per sak.
 */
sealed interface MicrofrontendFeil {
    val throwable: Throwable

    /** Kall mot Kafka (TMS microfrontend-topic) feilet. */
    data class KafkaFeil(override val throwable: Throwable) : MicrofrontendFeil

    /** Kall mot databasen feilet. */
    data class DatabaseFeil(override val throwable: Throwable) : MicrofrontendFeil
}
