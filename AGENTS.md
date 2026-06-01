# AGENTS.md — tiltakspenger-meldekort-api

Dette repoet følger monorepo-konvensjonene i [`../AGENTS.md`](../AGENTS.md) og Kotlin/JVM-backendkonvensjonene i [`../AGENTS-backend.md`](../AGENTS-backend.md). Les disse først.

## Testing

Se de generelle DB-/ende-til-ende-konvensjonene i [`../AGENTS-backend.md`](../AGENTS-backend.md#ende-til-ende-og-databasetester). Konkrete håndtak i dette repoet:

- **Route-tester med ekte Postgres:** `withTestApplicationContextAndPostgres { tac -> … }` (in-memory-varianten er `withTestApplicationContext`). Hjelpere som `mottaSakRequest(...)` og `sendInnNesteMeldekort(...)` sender ekte requests inn på route-laget.
- **Isolasjon:** `withTestApplicationContextAndPostgres(runIsolated = true)` (og `withMigratedDb(runIsolated = true)` for repo-tester). Standard er `runIsolated = false` — bruk isolasjon **kun** for aggregerte/på-tvers-av-sak-tester (typisk varsel-/microfrontend-jobber som spør på tvers av saker via `KjørJobberForTester`).
- **Delte generatorer:** `IdGenerators` (saksnummer, fnr, journalpostId) holdes av `TestDatabaseManager` og injiseres inn i test-konteksten. I tester:
  - route-/kontekst-tester: `tac.nesteFnr()` og `tac.nesteSaksnummer()`
  - repo-tester (`withMigratedDb { helper -> … }`): `helper.nesteFnr()` / `helper.nesteSaksnummer()`
  - **ikke** bruk `Fnr.random()` eller statiske verdier som `FAKE_FNR` i nye, ikke-isolerte tester — gi hver sak/person en unik, deterministisk id.
- `DokarkivClientFake` bruker en injisert `JournalpostIdGenerator` (sekvensiell i test, tilfeldig lokalt) i stedet for en konstant journalpostId.

