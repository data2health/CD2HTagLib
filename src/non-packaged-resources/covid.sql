CREATE SCHEMA covid;

CREATE MATERIALIZED VIEW covid.article AS
 SELECT article.pmid,
    (COALESCE(article.pub_date_year, "substring"(article.pub_date_medline, '[0-9][0-9][0-9][0-9]'::text)))::integer AS year,
    article_title.article_title
   FROM (medline.article
     JOIN medline.article_title USING (pmid))
  ;


CREATE MATERIALIZED VIEW covid.coronavirus AS
 SELECT DISTINCT pmid
   FROM (
        SELECT pmid FROM medline.abstract WHERE abstract ~ '[cC]oronavir'
   UNION
        SELECT pmid FROM medline.article_title WHERE article_title ~ '[cC]oronavir'
   UNION
        SELECT pmid FROM medline.chemical WHERE name_of_substance ~ '[cC]oronavir'
   UNION
        SELECT pmid FROM medline.keyword WHERE keyword ~ '[cC]oronavir'
   UNION
        SELECT pmid FROM medline.mesh_heading WHERE descriptor_name ~ '[cC]oronavir'
   ) AS foo
  ;

CREATE MATERIALIZED VIEW covid.inosine AS
 SELECT DISTINCT pmid
   FROM (
        SELECT pmid FROM medline.abstract WHERE abstract ~ '[iI]nosine'
   UNION
        SELECT pmid FROM medline.article_title WHERE article_title ~ '[iI]nosine'
   UNION
        SELECT pmid FROM medline.chemical WHERE name_of_substance ~ '[iI]nosine'
   UNION
        SELECT pmid FROM medline.keyword WHERE keyword ~ '[iI]nosine'
   UNION
        SELECT pmid FROM medline.mesh_heading WHERE descriptor_name ~ '[iI]nosine'
   ) AS foo
  ;

CREATE MATERIALIZED VIEW covid.inosine_pranobex AS
 SELECT DISTINCT pmid
   FROM (
        SELECT pmid FROM medline.abstract WHERE abstract ~ '[iI]nosine [pP]ranobex'
   UNION
        SELECT pmid FROM medline.article_title WHERE article_title ~ '[iI]nosine [pP]ranobex'
   UNION
        SELECT pmid FROM medline.chemical WHERE name_of_substance ~ '[iI]nosine [pP]ranobex'
   UNION
        SELECT pmid FROM medline.keyword WHERE keyword ~ '[iI]nosine [pP]ranobex'
   UNION
        SELECT pmid FROM medline.mesh_heading WHERE descriptor_name ~ '[iI]nosine [pP]ranobex'
   ) AS foo
  ;

CREATE MATERIALIZED VIEW covid.inosine_acedoben_dimepranol AS
 SELECT DISTINCT pmid
   FROM (
        SELECT pmid FROM medline.abstract WHERE abstract ~ '[iI]nosine [aA]cedoben [dD]imepranol'
   UNION
        SELECT pmid FROM medline.article_title WHERE article_title ~ '[iI]nosine [aA]cedoben [dD]imepranol'
   UNION
        SELECT pmid FROM medline.chemical WHERE name_of_substance ~ '[iI]nosine [aA]cedoben [dD]imepranol'
   UNION
        SELECT pmid FROM medline.keyword WHERE keyword ~ '[iI]nosine [aA]cedoben [dD]imepranol'
   UNION
        SELECT pmid FROM medline.mesh_heading WHERE descriptor_name ~ '[iI]nosine [aA]cedoben [dD]imepranol'
   ) AS foo
  ;
  
CREATE MATERIALIZED VIEW covid.isoprinosine AS
 SELECT DISTINCT pmid
   FROM (
        SELECT pmid FROM medline.abstract WHERE abstract ~ '[iI]soprinosine'
   UNION
        SELECT pmid FROM medline.article_title WHERE article_title ~ '[iI]soprinosine'
   UNION
        SELECT pmid FROM medline.chemical WHERE name_of_substance ~ '[iI]soprinosine'
   UNION
        SELECT pmid FROM medline.keyword WHERE keyword ~ '[iI]soprinosine'
   UNION
        SELECT pmid FROM medline.mesh_heading WHERE descriptor_name ~ '[iI]soprinosine'
   ) AS foo
  ;
  
CREATE MATERIALIZED VIEW covid.methisoprinol AS
 SELECT DISTINCT pmid
   FROM (
        SELECT pmid FROM medline.abstract WHERE abstract ~ '[mM]ethisoprinol'
   UNION
        SELECT pmid FROM medline.article_title WHERE article_title ~ '[mM]ethisoprinol'
   UNION
        SELECT pmid FROM medline.chemical WHERE name_of_substance ~ '[mM]ethisoprinol'
   UNION
        SELECT pmid FROM medline.keyword WHERE keyword ~ '[mM]ethisoprinol'
   UNION
        SELECT pmid FROM medline.mesh_heading WHERE descriptor_name ~ '[mM]ethisoprinol'
   ) AS foo
  ;

CREATE MATERIALIZED VIEW covid.inosine_pranobex_sans_mesh AS
 SELECT DISTINCT pmid
   FROM (
        SELECT pmid FROM medline.abstract WHERE abstract ~ '[iI]nosine [pP]ranobex'
   UNION
        SELECT pmid FROM medline.article_title WHERE article_title ~ '[iI]nosine [pP]ranobex'
   UNION
        SELECT pmid FROM medline.chemical WHERE name_of_substance ~ '[iI]nosine [pP]ranobex'
   UNION
        SELECT pmid FROM medline.keyword WHERE keyword ~ '[iI]nosine [pP]ranobex'
   ) AS foo
   WHERE pmid NOT IN (SELECT pmid FROM medline.mesh_heading WHERE descriptor_name ~ '[iI]nosine [pP]ranobex')
  ;

CREATE INDEX apmid ON covid.article USING btree (pmid);
CREATE INDEX cpmid ON covid.coronavirus USING btree (pmid);
CREATE INDEX ipmid ON covid.inosine USING btree (pmid);
CREATE INDEX ippmid ON covid.inosine_pranobex USING btree (pmid);
CREATE INDEX ipsmpmid ON covid.inosine_pranobex_sans_mesh USING btree (pmid);
CREATE INDEX iadpmid ON covid.inosine_acedoben_dimepranol USING btree (pmid);
CREATE INDEX isopmid ON covid.isoprinosine USING btree (pmid);
CREATE INDEX methpmid ON covid.methisoprinol USING btree (pmid);
