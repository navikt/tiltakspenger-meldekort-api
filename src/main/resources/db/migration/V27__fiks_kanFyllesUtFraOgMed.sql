/*
 Tidligere migrering antok at varsel_id var utfyllt for alle meldeperioder som var tilbake i tid.
 Det viser seg at det ikke stemte. Noen av dem hadde varsel_id som null fordi dem var deaktivert, eller andre grunner.

 Etter litt undersøking - fant vi ut at vi kan migrere alle radene før 2025-11-24 15:02:00 til kl 00:00:00.
 Alle som er etter denne datoen vil skal være migrert riktig.
 */

update meldeperiode
set kan_fylles_ut_fra_og_med = (til_og_med::date - 2 || ' 00:00:00')::TIMESTAMP
WHERE kan_fylles_ut_fra_og_med < TIMESTAMP '2025-11-24 15:02:00';