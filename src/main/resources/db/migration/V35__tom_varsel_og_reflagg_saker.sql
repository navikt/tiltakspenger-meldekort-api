-- Varsel-tabellen er fortsatt ubrukt i prod. Tøm den før varseljobben bygger opp riktig tilstand på nytt.
DELETE FROM varsel;

-- Reflagg alle saker slik at de vurderes for varsel på nytt etter tømmingen.
UPDATE sak
SET skal_vurdere_varsel = true,
    sist_vurdert_varsel = NULL,
    sist_flagget_tidspunkt = NULL;