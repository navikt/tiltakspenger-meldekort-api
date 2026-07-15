package no.nav.tiltakspenger.meldekort.infra.db

/**
 * Kastes når en optimistisk lås (versjon eller tidspunkt) ikke matcher mellom det jobben/tjenesten leste og det som finnes i databasen ved oppdateringstidspunktet.
 *
 * Brukes for å rulle tilbake transaksjonen og la saken plukkes opp på nytt i neste kjøring med oppdatert datagrunnlag, slik at vi ikke mister hendelser eller skriver over endringer gjort av en konkurrerende transaksjon.
 *
 * Bevisst en `RuntimeException` (ikke en `Either`-feil): dette er en uventet samtidighet vi ikke skal håndtere lokalt – vi vil at hele transaksjonen ruller tilbake og at jobben prøver på nytt.
 */
class OptimistiskLåsFeil(message: String) : RuntimeException(message)
