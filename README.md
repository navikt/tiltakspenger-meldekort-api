tiltakspenger-meldekort-api
================
Håndterer meldekortene som sendes inn for de som mottar tiltakspenger fra Nav ([Forskrift om tiltakspenger mv. (tiltakspengeforskriften) - § 5.Meldeplikt](https://lovdata.no/forskrift/2013-11-04-1286/§5)). For å motta tiltakspenger må man meldekortet sendes inn hver 14. dag.

Dette er backenden til [tiltakspenger-meldekort](https://github.com/navikt/tiltakspenger-meldekort). 

En del av satsningen ["Flere i arbeid – P4"](https://memu.no/artikler/stor-satsing-skal-fornye-navs-utdaterte-it-losninger-og-digitale-verktoy/)

# Komme i gang
Klone repoet. Vi anbefaler at man setter opp via meta-repoet: https://github.com/navikt/tiltakspenger-meldekort
## Forutsetninger
- [JDK](https://jdk.java.net/)
- [Kotlin](https://kotlinlang.org/)
- [Gradle](https://gradle.org/) brukes som byggeverktøy og er inkludert i oppsettet

For hvilke versjoner som brukes, [se byggefilen](build.gradle.kts)

## Bygging og denslags
For å bygge artifaktene:

```sh
./gradlew build
```

---

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #tpts-tech.
