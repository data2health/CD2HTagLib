create materialized view filter as
select id,email from aminer_author,n3c_user
where (name ~ ('^'||first_initial||'[ .].*'||last_name||'$')::text
   or name ~ ('^'||first_name||' .*'||last_name||'$')::text
   or name ~ ('^'||last_name||' '||first_initial||'([ .].*)?$')::text
   or name ~ ('^'||last_name||' '||first_name||'( .*)?$')::text) and last_name='Eichmann'
;

create materialized view filter as
select * from aminer_author
where exists ( select * from n3c_user where name ~ ('^'||first_initial||'[ .].*'||last_name||'$')::text
   or name ~ ('^'||first_name||' .*'||last_name||'$')::text
   or name ~ ('^'||last_name||' '||first_initial||'([ .].*)?$')::text
   or name ~ ('^'||last_name||' '||first_name||'( .*)?$')::text) and name ~ 'Eichmann'
;

create table oag_n3c.aminer_author as select * from oag.aminer_author where id in (select id from oag_n3c.filter2);
create table oag_n3c.aminer_author_org as select * from oag.aminer_author_org where id in (select id from oag_n3c.filter2);
create table oag_n3c.aminer_author_pub as select * from oag.aminer_author_pub where id in (select id from oag_n3c.filter2);
create table oag_n3c.aminer_author_tag as select * from oag.aminer_author_tag where id in (select id from oag_n3c.filter2);

create table oag_n3c.aminer_paper_author as select * from oag.aminer_paper_author where author_id in (select id from oag_n3c.filter2);
create table oag_n3c.aminer_paper as select distinct * from oag.aminer_paper where id in (select id from oag_n3c.aminer_paper_author);
create table oag_n3c.aminer_paper_keyword as select distinct * from oag.aminer_paper_keyword where id in (select id from oag_n3c.aminer_paper_author);
create table oag_n3c.aminer_paper_url as select distinct * from oag.aminer_paper_url where id in (select id from oag_n3c.aminer_paper_author);

create table oag_n3c.aminer_venue as select distinct * from oag.aminer_venue where id in (select venue_id from oag_n3c.aminer_paper);


select year,keyword,count(*)
from aminer_paper,aminer_paper_keyword
where aminer_paper.id=aminer_paper_keyword.id
  and aminer_paper.id in (select id
  						  from aminer_paper_author
  						  where author_id in (select id
  						  					  from n3c_user natural join user_map natural join aminer_author
  						  					  where last_name='Saltz'
  						  					  )
  						  )
group by 1,2 having count(*) > 1
order by 1 desc, 3 desc;
