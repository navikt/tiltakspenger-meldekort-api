-- Siste meldekort før jul kan fylles ut tidligere enn fredag (se Meldeperiode.kanFyllesUtFraOgMed):
-- fra siste onsdag i meldeperioden kl. 15, men senest siste hverdag som er minst 7 dager før julaften (17. desember eller tidligere).
-- Siste meldeperiode før jul er meldeperioden som slutter 11.–24. desember.
-- Nye meldeperioder får riktig tidspunkt ved mottak, men meldeperioder som allerede er mottatt oppdateres ikke ved nytt mottak og må derfor migreres.
-- Historiske meldeperioder (2025 og tidligere) røres ikke; 2025 ble håndtert i V28.
-- Sakene flagges for ny varselvurdering slik at allerede planlagte varsler flyttes til det nye tidspunktet.
WITH julemeldeperioder AS (
    SELECT id,
           til_og_med,
           make_date(EXTRACT(YEAR FROM til_og_med)::INT, 12, 17) AS syttende_desember
    FROM meldeperiode
    WHERE til_og_med >= DATE '2026-01-01'
      AND til_og_med BETWEEN make_date(EXTRACT(YEAR FROM til_og_med)::INT, 12, 11)
                         AND make_date(EXTRACT(YEAR FROM til_og_med)::INT, 12, 24)
),
nye_tidspunkter AS (
    SELECT id,
           LEAST(
               til_og_med - ((EXTRACT(ISODOW FROM til_og_med)::INT - 3 + 7) % 7),
               syttende_desember - CASE EXTRACT(ISODOW FROM syttende_desember)::INT
                                       WHEN 6 THEN 1
                                       WHEN 7 THEN 2
                                       ELSE 0
                                   END
           ) + TIME '15:00' AS kan_fylles_ut_fra_og_med
    FROM julemeldeperioder
),
oppdaterte AS (
    UPDATE meldeperiode mp
    SET kan_fylles_ut_fra_og_med = n.kan_fylles_ut_fra_og_med
    FROM nye_tidspunkter n
    WHERE mp.id = n.id
      AND mp.kan_fylles_ut_fra_og_med > n.kan_fylles_ut_fra_og_med
    RETURNING mp.sak_id
)
UPDATE sak
SET skal_vurdere_varsel = true,
    sist_flagget_tidspunkt = clock_timestamp()
WHERE id IN (SELECT sak_id FROM oppdaterte);
