-- Script for å slette en sak (person) fra tiltakspenger-meldekort-api i dev
WITH sakz AS (DELETE FROM sak WHERE id = 'sak_01JQVBMZ7WX2VKEEK8XKV00WJ4' RETURNING id),
     meldekort_brukerz AS (DELETE from meldekort_bruker mb WHERE sak_id IN (SELECT id FROM sakz)),
     meldeperiode AS (delete from meldeperiode m WHERE sak_id IN (SELECT id FROM sakz))
SELECT id
FROM sakz;
