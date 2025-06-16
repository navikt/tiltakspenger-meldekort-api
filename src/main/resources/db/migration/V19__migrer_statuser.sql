
UPDATE meldekort_bruker
SET dager = replace(
    dager::text,
    '"FRAVÆR_VELFERD_GODKJENT_AV_NAV"',
    '"FRAVÆR_GODKJENT_AV_NAV"'
)::jsonb
WHERE dager::text LIKE '%FRAVÆR_VELFERD_GODKJENT_AV_NAV%';

UPDATE meldekort_bruker
SET dager = replace(
    dager::text,
    '"FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV"',
    '"FRAVÆR_ANNET"'
)::jsonb
WHERE dager::text LIKE '%FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV%';

UPDATE meldekort_bruker
SET dager = replace(
    dager::text,
    '"IKKE_REGISTRERT"',
    '"IKKE_BESVART"'
)::jsonb
WHERE dager::text LIKE '%IKKE_REGISTRERT%';
