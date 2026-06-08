tiltakspenger-meldekort-api
================
Håndterer meldekortene som sendes inn for de som mottar tiltakspenger fra Nav ([Forskrift om tiltakspenger mv. (tiltakspengeforskriften) - § 5.Meldeplikt](https://lovdata.no/forskrift/2013-11-04-1286/§5)). For å motta tiltakspenger må man sende inn et meldekort hver 14. dag.

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

## Lokal kjøring
Appen kan startes lokalt med main-funksjonen i LokalMain.kt. Som default krever denne kun database kjørende lokalt, andre eksterne tjenester erstattes av fakes.

Dersom du ønsker å kjøre ekte autentiseringsflyt lokalt, sett miljøvariabelen `BRUK_FAKE_AUTH=false`, f.eks. via run config i Intellij.

# Arkitektur og pakkestruktur

Prosjektet er organisert etter feature, med DDD- og heksagonale prinsipper som rettesnor. Det betyr at kode først og fremst skal ligge nær den delen av domenet den tilhører, ikke i generelle tekniske lag som `routes`, `clients` eller `repository` på toppnivå.

Målet er at domenet skal være lett å forstå uten å måtte lese teknisk integrasjonskode. Domenekoden beskriver begreper, regler, tilstander, kommandoer, porter og applikasjonsflyt. Tekniske detaljer legges i `infra`.

## Avhengighetsretning

Avhengigheter skal peke innover:

- Domenet kan definere porter/interfaces, for eksempel `MeldekortRepo`, `SakRepo`, `VarselClient` og `SaksbehandlingClient`.
- `infra` implementerer portene og oversetter mellom tekniske formater og domenemodeller.
- Domenet skal ikke importere `infra`.
- Applikasjonsoppsett og wiring binder domenet sammen med konkrete adaptere i `infra`.

Dette håndheves også av arkitekturtester, blant annet:

- `src/test/kotlin/no/nav/tiltakspenger/arkitektur/InfraImportKonsistTest.kt`
- `src/test/kotlin/no/nav/tiltakspenger/arkitektur/DomainImportWhitelistKonsistTest.kt`

## Pakkeprinsipp

Bruk denne strukturen for nye features og ved opprydding i eksisterende kode:

```text
no.nav.tiltakspenger.meldekort.<feature>
no.nav.tiltakspenger.meldekort.<feature>.infra
no.nav.tiltakspenger.meldekort.<feature>.infra.routes
```

Eksempler:

```text
meldekort/meldekort
meldekort/meldekort/infra
meldekort/meldekort/infra/routes

meldekort/sak
meldekort/sak/infra
meldekort/sak/infra/routes

meldekort/varsler
meldekort/varsler/infra

meldekort/journalfring
meldekort/journalfring/infra
```

Felles teknisk applikasjonskode ligger i:

```text
meldekort/infra
meldekort/infra/routes
meldekort/infra/db
```

Dette gjelder for eksempel app-start, Ktor-oppsett, konfigurasjon, dependency wiring, felles HTTP-klient og databaseoppsett.

## Hva hører hjemme i domenet?

Domenepakken til en feature skal inneholde kode som uttrykker forretningsbegreper og regler:

- Domenemodeller og verdityper, for eksempel `Meldekort`, `MeldekortDag`, `Meldeperiode`, `Sak`, `Varsel`.
- Domeneoperasjoner og tjenester, for eksempel `HentMeldekortService`, `LagreMeldekortFraBrukerService`, `KorrigerMeldekortService`, `SendMeldekortService`, `LagreFraSaksbehandlingService`, `VurderVarselService`.
- Porter/interfaces som domenet trenger for å snakke med omverdenen, for eksempel `MeldekortRepo`, `SakRepo`, `VarselRepo`, `VarselClient`, `PdfgenClient`.
- Kommandoer og domeneutfall, for eksempel `KorrigerMeldekortCommand` og `VurderVarselUtfall`.

Domenet skal i minst mulig grad kjenne til transport, database, Ktor, Kafka, HTTP, JSON eller konkrete klientbiblioteker.

## Hva hører hjemme i `infra`?

`infra` er adapterlaget mot tekniske detaljer og eksterne systemer. Kode i `infra` kan kjenne til domenet, men domenet skal ikke kjenne til `infra`.

Legg dette i `infra`:

### Serialisering og deserialisering

All kode som er tett knyttet til teknisk format eller mapping mellom teknisk representasjon og domene hører hjemme i `infra`.

Eksempler:

- Ktor/Jackson-oppsett: `meldekort/infra/routes/MeldekortApi.kt`
- Deserialisering av HTTP-body i ruter: `meldekort/meldekort/infra/routes/SendInnMeldekortRoute.kt`
- Database-JSON og mapping til/fra DB-format: `meldekort/meldekort/infra/MeldekortDagDbJson.kt`
- DTO-er for PDF-/brevformat: `meldekort/journalfring/infra/BrevMeldekortDTO.kt` og `BrevMeldekortDagDTO.kt`

### Route-kode og route-DTO-er

Ktor-ruter, request-/response-DTO-er og HTTP-status-/header-håndtering hører hjemme i `infra.routes` under feature-pakken.

Eksempler:

- Bruker-ruter: `meldekort/bruker/infra/routes/HentBrukerRoute.kt`
- Meldekort-ruter: `meldekort/meldekort/infra/routes/SendInnMeldekortRoute.kt`, `HentMeldekortRoute.kt`, `HentInnsendteMeldekortRoute.kt`
- Korrigering: `meldekort/meldekort/infra/routes/korrigering`
- Landingsside: `meldekort/landingsside/infra/routes/FellesLandingssideRoutes.kt`
- Microfrontend: `meldekort/microfrontend/infra/routes/MicrofrontendRoutes.kt` og `MicrofrontendKortDTO.kt`
- Saksbehandling: `meldekort/sak/infra/routes/SakFraSaksbehandlingRoute.kt`

Route-kode skal være tynn: autentisering, parsing, validering av transportformat, kall til domenet/service og mapping til HTTP-respons. Forretningsregler skal flyttes inn i domenemodellen eller domenetjenesten som eier dataene.

### HTTP-klienter og eksterne DTO-er

HTTP-klientimplementasjoner og DTO-er som representerer eksterne API-er hører hjemme i `infra`.

Eksempler:

- Port i domenet: `meldekort/sak/SaksbehandlingClient.kt`
- HTTP-adapter: `meldekort/sak/infra/SaksbehandlingClientImpl.kt`
- Ekstern DTO: `meldekort/sak/infra/SaksbehandlingMeldekortDTO.kt`
- Dokarkiv-adapter: `meldekort/journalfring/infra/DokarkivClientImpl.kt`
- Dokarkiv request-format: `meldekort/journalfring/infra/JournalpostRequest.kt`
- Pdfgen-adapter: `meldekort/journalfring/infra/PdfgenClientImpl.kt`
- Arena-adapter: `meldekort/arena/infra/ArenaMeldekortHttpClient.kt`

Domenet bør kun forholde seg til porten og domenemodeller. Adapteren mapper mellom ekstern DTO og domenemodell.

### Kafka, topic-håndtering og Kafka-DTO-er

Kafka-consumere, Kafka-producere, topic-navn, commit-/retry-håndtering og meldingsformat hører hjemme i `infra`.

Eksempler:

- Kafka consumer: `meldekort/identhendelse/infra/IdenthendelseConsumer.kt`
- Kafka producer-adapter for microfrontend: `meldekort/microfrontend/infra/TmsMikrofrontendClientImpl.kt`
- Kafka producer-adapter for varsler: `meldekort/varsler/infra/TmsVarselClientImpl.kt`
- Kafka wiring og topic-konfigurasjon: `meldekort/infra/ApplicationContext.kt` og `meldekort/infra/Configuration.kt`

Kafka-spesifikke DTO-er skal ligge i relevant `<feature>.infra`. Dersom meldingen inneholder informasjon domenet trenger, mapper adapteren meldingen til en domenekommando eller domenemodell før domenet kalles.

### Repositories og databasekode

Repository-interfacet ligger i domenet, mens databaseimplementasjonen ligger i `infra`.

Eksempler:

- Domeneport: `meldekort/meldekort/MeldekortRepo.kt`
- Postgres-adapter: `meldekort/meldekort/infra/MeldekortPostgresRepo.kt`
- Domeneport: `meldekort/meldeperiode/MeldeperiodeRepo.kt`
- Postgres-adapter: `meldekort/meldeperiode/infra/MeldeperiodePostgresRepo.kt`
- Domeneport: `meldekort/sak/SakRepo.kt`
- Postgres-adapter og DB-format: `meldekort/sak/infra/SakPostgresRepo.kt` og `SakDb.kt`
- Varsel-repositories: `meldekort/varsler/infra/VarselPostgresRepo.kt`, `VarselMeldekortPostgresRepo.kt`, `SakVarselPostgresRepo.kt`
- Felles databaseoppsett: `meldekort/infra/db/DataSourceSetup.kt` og `FlywayMigrate.kt`

SQL, transaksjoner, DB-spesifikke enum-/JSON-formater og mapping mellom rader og domenemodeller skal ikke lekke inn i domenet.

### Konfigurasjon, wiring og teknisk applikasjonsoppsett

Teknisk oppstart og sammenkobling av adaptere hører hjemme i felles `infra`.

Eksempler:

- App-start og Ktor-moduler: `meldekort/infra/Application.kt`
- Dependency wiring: `meldekort/infra/ApplicationContext.kt`
- Miljøvariabler og config: `meldekort/infra/Configuration.kt`
- Felles HTTP-klientoppsett: `meldekort/infra/KonfigurertHttpClient.kt`

## Teststruktur

Tester skal speile produksjonsstrukturen. Hvis produksjonskode ligger i `meldekort/varsler/infra`, skal testen ligge i tilsvarende struktur under `src/test/kotlin`. Dette gjør det enklere å se hvilke tester som hører til hvilken feature og unngår parallelle strukturer basert på tekniske lag.

Eksempler:

- `meldekort/journalfring/infra/BrevMeldekortDTOTest.kt`
- `meldekort/varsler/VarslerTest.kt`
- `meldekort/meldekort/infra/HentMeldekortRepoTest.kt`
- `meldekort/meldekort/infra/routes/SendInnMeldekortRouteTest.kt`

## Tommelfingerregler

- Hvis koden handler om regler og begreper i tiltakspenger/meldekort-domenet, legg den i feature-pakken uten `infra`.
- Hvis koden handler om hvordan vi snakker HTTP, Kafka, SQL, JSON, Ktor, Jackson, Postgres eller eksterne API-er, legg den i `infra`.
- Hvis en klasse både inneholder teknisk adapterkode og forretningsregler, trekk forretningsreglene inn i domenet og la adapteren mappe inn/ut.
- Nye boundary-DTO-er for routes, HTTP-klienter, Kafka og database skal som hovedregel ligge i `infra`.
- Porter/interfaces eies av domenet; implementasjonene eies av `infra`.

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #tpts-tech.
