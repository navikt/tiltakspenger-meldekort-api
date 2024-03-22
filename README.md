tiltakspenger
================

Håndterer...

En del av satsningen ["Flere i arbeid – P4"](https://memu.no/artikler/stor-satsing-skal-fornye-navs-utdaterte-it-losninger-og-digitale-verktoy/)

# Komme i gang
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

### Kjøre opp appen lokalt

Appen har alle miljøvariabler som trenger for lokal kjøring nedfelt i `Configuration.kt`, så det er ikke nødvendig å
sette egne miljøvariabler for å kjøre opp appen lokalt. Kjør som vanlig opp `main`-funksjonen i `Application.kt` for å kjøre
opp appen. Appen krever en database for å kjøre, så sørg for at du har en database kjørende på `5430` før du kjører opp appen.

Det anbefales å kjøre opp hele verdikjeden med docker compose. Se [meta-repoet for tiltakspenger](https://github.com/navikt/tiltakspenger)
for informasjon om hvordan dette gjøres.

---

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #tpts-tech.
