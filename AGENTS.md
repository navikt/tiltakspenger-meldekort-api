# AGENTS.md — tiltakspenger-meldekort-api

Dette repoet følger monorepo-konvensjonene i [`../AGENTS.md`](../AGENTS.md) og Kotlin/JVM-backendkonvensjonene i [`../AGENTS-backend.md`](../AGENTS-backend.md). Les disse først.

## Kodekonvensjoner

- **Ikke bruk `internal`.** Dette er en enmodul-app (deployes som én artifact, ikke som bibliotek). Kotlin har ingen package-private, og `internal` betyr «synlig i hele modulen» — altså hele appen. I praksis gir det derfor ingen ekstra innkapsling utover `public` ved pakkegrenser, og en blanding av `internal` noen få steder skaper bare inkonsistens og falsk trygghet. Bruk `public` (standard) og `private`. Innkapsling mellom domeneområder håndheves gjennom pakkestruktur, porter/interfaces og disiplin — ikke gjennom `internal`.

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

