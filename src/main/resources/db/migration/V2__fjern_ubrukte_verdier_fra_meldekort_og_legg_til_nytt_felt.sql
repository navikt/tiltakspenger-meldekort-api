alter table meldekort
drop column rammevedtak_id,
drop column forrige_meldekort_id;

alter table meldekort
add meldeperiode_id varchar not null;