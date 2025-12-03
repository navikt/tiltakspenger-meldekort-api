-- Meldekort for siste meldeperiode før jul skal kunne sendes fra onsdag istedenfor fredag
update meldeperiode
set kan_fylles_ut_fra_og_med = '2025-12-17 15:00:00'
where fra_og_med = '2025-12-08'