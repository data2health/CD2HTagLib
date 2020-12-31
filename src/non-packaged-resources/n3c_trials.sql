create schema n3c_trials;

create materialized view n3c_trials.covid_id as
select distinct id from
 (select id from clinical_trials.study 
  where start_date~'202[01]'
    and (official_title ~'[cC][oO][vV][^eE]' or official_title~'[cC]oronavirus')
 union
  select id from clinical_trials.condition
  where id in (select id from study where start_date~'202[01]')
    and (condition~'[cC][oO][vV][^eE]') or condition~'[cC]oronavirus'
 ) as foo;
 
create index cidid on n3c_trials.covid_id(id);

create materialized view n3c_trials.study as
select * from clinical_trials.study where id in (select id from n3c_trials.covid_id);

create index study_id on n3c_trials.study(id);

create materialized view n3c_trials.condition as
select * from clinical_trials.condition where id in (select id from n3c_trials.covid_id);

create index condition_id on n3c_trials.condition(id);

create materialized view n3c_trials.condition_mesh as
select * from clinical_trials.condition_mesh where id in (select id from n3c_trials.covid_id);

create index condition_mesh_id on n3c_trials.condition_mesh(id);

create materialized view n3c_trials.intervention as
select * from clinical_trials.intervention where id in (select id from n3c_trials.covid_id);

create index intervention_id on n3c_trials.intervention(id);

create materialized view n3c_trials.intervention_mesh as
select * from clinical_trials.intervention_mesh where id in (select id from n3c_trials.covid_id);

create index intervention_mesh_id on n3c_trials.intervention_mesh(id);

create materialized view n3c_trials.overall_official as
select * from clinical_trials.overall_official where id in (select id from n3c_trials.covid_id);

create index overall_official_id on n3c_trials.overall_official(id);

create materialized view n3c_trials.central_contact as
select * from clinical_trials.central_contact where id in (select id from n3c_trials.covid_id);

create index central_contact_id on n3c_trials.central_contact(id);

create materialized view n3c_trials.collaborator as
select * from clinical_trials.collaborator where id in (select id from n3c_trials.covid_id);

create index collaborator_id on n3c_trials.collaborator(id);

create materialized view n3c_trials.location as
select * from clinical_trials.location where id in (select id from n3c_trials.covid_id);

create index location_id on n3c_trials.location(id);

create materialized view n3c_trials.location_contact as
select * from clinical_trials.location_contact where id in (select id from n3c_trials.covid_id);

create index location_contact_id on n3c_trials.location_contact(id);
