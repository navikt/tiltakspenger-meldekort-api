ALTER TABLE meldeperiode DROP CONSTRAINT unique_kjede_id_opprettet;
ALTER TABLE meldeperiode DROP CONSTRAINT unique_kjede_id_versjon;

ALTER TABLE meldeperiode ADD CONSTRAINT unique_kjede_id_opprettet UNIQUE (sak_id, kjede_id, opprettet);
ALTER TABLE meldeperiode ADD CONSTRAINT unique_kjede_id_versjon UNIQUE (sak_id, kjede_id, versjon);