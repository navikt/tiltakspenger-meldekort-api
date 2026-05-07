-- Tillat flere pågående inaktiveringer per sak. Domenet begrenser fortsatt SkalAktiveres/Aktiv
-- til maks ett pågående varsel per sak, men inaktiveringsjobben håndterer nå 0-N
-- varsler i tilstand SkalInaktiveres.
DROP INDEX idx_varsel_unik_skal_inaktiveres_per_sak;

-- Varsel-tabellen er fortsatt ubrukt i prod. Tøm den før varseljobben bygger opp riktig tilstand på nytt.
DELETE FROM varsel;

-- Reflagg alle saker slik at de vurderes for varsel på nytt etter tømmingen.
UPDATE sak
SET skal_vurdere_varsel = true,
    sist_vurdert_varsel = NULL,
    sist_flagget_tidspunkt = NULL;