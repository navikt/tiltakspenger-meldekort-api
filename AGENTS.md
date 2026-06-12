# AGENTS.md — tiltakspenger-meldekort-api

Dette repoet følger monorepo-konvensjonene i [`../AGENTS.md`](../AGENTS.md) og Kotlin/JVM-backendkonvensjonene i [`../AGENTS-backend.md`](../AGENTS-backend.md). Les disse først.

## Kodekonvensjoner

- **Ikke bruk `internal`.** Dette er en enmodul-app (deployes som én artifact, ikke som bibliotek). Kotlin har ingen package-private, og `internal` betyr «synlig i hele modulen» — altså hele appen. I praksis gir det derfor ingen ekstra innkapsling utover `public` ved pakkegrenser, og en blanding av `internal` noen få steder skaper bare inkonsistens og falsk trygghet. Bruk `public` (standard) og `private`. Innkapsling mellom domeneområder håndheves gjennom pakkestruktur, porter/interfaces og disiplin — ikke gjennom `internal`.

## Database og migreringer

- **Rullende deploy med flere pod-er — bruk expand/contract for skjemaendringer.** Appen kjører med flere pod-er, og under deploy kjører en ny pod Flyway-migreringen mens gamle pod-er fortsatt serverer trafikk på det gamle skjemaet. Hver migrering må derfor være **bakoverkompatibel med forrige deploys kode**. Destruktive endringer (drop/rename av kolonne, ny `NOT NULL`, ny `FK` på eksisterende data, flytte data mellom tabeller) kan **ikke** gjøres i én deploy — de må splittes over flere:
  1. **Expand:** legg til det nye (tabell/kolonne) og begynn å dual-skrive (gammelt + nytt). Les fortsatt fra det gamle.
  2. **Backfill:** idempotent reconcile (`INSERT … SELECT … ON CONFLICT DO NOTHING`) når alle gamle pod-er er ute, så den blir komplett og kan kjøres på nytt.
  3. **Flytt lesing** til det nye. Fortsatt dual-write.
  4. **Stopp skriving** til det gamle (drop ev. `NOT NULL` først så gamle pod-er ikke knekker).
  5. **Contract:** drop den gamle kolonnen/tabellen.
  Hvert steg må være fullt utrullet før neste starter. Backfill legges bevisst *etter* at dual-write er på alle pod-er — ellers blir den ufullstendig fordi gamle pod-er fortsatt skriver kun til det gamle skjemaet.
- **kotliquery `Row` er kun gyldig under iterasjon.** Ikke samle opp rå `Row`-objekter og les fra dem etter at ResultSet er lukket («ResultSet is closed»). Materialiser alt du trenger inn i egne data-klasser inne i `.map { … }`. Vær spesielt obs når en repo-mapper kaller en annen repo som åpner sitt eget ResultSet i samme session (f.eks. `MeldekortvedtakPostgresRepo.hentForSakId` som kalles nestet inne i `SakPostgresRepo.fromRow`) — den indre spørringen må materialisere fullt ut.

## Testing

Se de generelle DB-/ende-til-ende-konvensjonene i [`../AGENTS-backend.md`](../AGENTS-backend.md#ende-til-ende-og-databasetester). Konkrete håndtak i dette repoet:

- **Route-tester med ekte Postgres:** `withTestApplicationContextAndPostgres { tac -> … }` (in-memory-varianten er `withTestApplicationContext`). Hjelpere som `mottaSakRequest(...)` og `sendInnNesteMeldekort(...)` sender ekte requests inn på route-laget.
- **Isolasjon:** `withTestApplicationContextAndPostgres(runIsolated = true)` (og `withMigratedDb(runIsolated = true)` for repo-tester). Standard er `runIsolated = false` — bruk isolasjon **kun** for aggregerte/på-tvers-av-sak-tester (typisk varsel-/microfrontend-jobber som spør på tvers av saker via `KjørJobberForTester`).
- **Delte generatorer:** `IdGenerators` (saksnummer, fnr, journalpostId) holdes av `TestDatabaseManager` og injiseres inn i test-konteksten. I tester:
  - route-/kontekst-tester: `tac.nesteFnr()` og `tac.nesteSaksnummer()`
  - repo-tester (`withMigratedDb { helper -> … }`): `helper.nesteFnr()` / `helper.nesteSaksnummer()`
  - **ikke** bruk statiske, delte fnr-verdier i nye, ikke-isolerte tester — gi hver sak/person en unik, deterministisk id.
- **Innlogget bruker i route-tester:** `TexasClientFakeTest` dekoder selve bearer-token-et (JWT-en fra `JwtGenerator`) og autentiserer kallet som `pid`-claimet — akkurat som ekte Texas. Konteksten er derfor *ikke* låst til én person. Bruker-hjelperne (`landingssideStatusRequest(fnr = …)`, `hentBrukerMedSakRequest(fnr = …)`, `sendInnNesteMeldekort(fnr = …)` osv.) tar `fnr` **eksplisitt** når de skal autentisere som en konkret bruker. Bruk `val fnr = tac.nesteFnr()` i testen og send samme fnr både når du oppretter sak/meldekort og når du kaller bruker-routene. `mottaSakRequest(...)` kan lage et nytt deterministisk fnr selv for helt frittstående saker, men send eksplisitt `fnr` hvis saken senere skal leses/endres som bruker.
- `DokarkivClientFake` bruker en injisert `JournalpostIdGenerator` (sekvensiell i test, tilfeldig lokalt) i stedet for en konstant journalpostId.

