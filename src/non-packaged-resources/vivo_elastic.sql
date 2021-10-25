
CREATE SCHEMA vivo_elastic;

CREATE TABLE vivo_elastic.site (
    id integer PRIMARY KEY,
    site text,
    endpoint text,
    url text,
    x_coord integer,
    y_coord integer,
    harvest_time interval,
    description text,
    platform text,
    ctsa boolean,
    ontology_version text,
    sura boolean
);

CREATE TABLE vivo_elastic.person (
    url text PRIMARY KEY,
    label text,
    id integer,
    email text,
    phone text,
    title text,
	FOREIGN KEY (id) REFERENCES vivo_elastic.site(id) ON UPDATE CASCADE ON DELETE CASCADE    
);

CREATE TABLE vivo_elastic.person_keyword (
    url text,
    keyword text,
    PRIMARY KEY (url,keyword),
	FOREIGN KEY (url) REFERENCES vivo_elastic.person(url) ON UPDATE CASCADE ON DELETE CASCADE    
);

CREATE TABLE vivo_elastic.person_research_area (
    url text,
    research_area text,
    PRIMARY KEY (url,research_area),
	FOREIGN KEY (url) REFERENCES vivo_elastic.person(url) ON UPDATE CASCADE ON DELETE CASCADE    
);

CREATE TABLE vivo_elastic.person_research_overview (
    url text PRIMARY KEY,
    overview text,
	FOREIGN KEY (url) REFERENCES vivo_elastic.person(url) ON UPDATE CASCADE ON DELETE CASCADE    
);

CREATE VIEW vivo_elastic.staging_person AS
SELECT DISTINCT ON (person_real.uri)
	uri AS url,
    ((first_name || ' '::text) || last_name) AS label,
    id,
    email,
    phone,
    title
FROM vivo_aggregated.person_real
WHERE id in (SELECT id from vivo_elastic.site);

CREATE VIEW vivo_elastic.staging_person_keyword AS
SELECT DISTINCT
	uri AS url,
    value AS keyword
FROM vivo_aggregated.person_property_freetextkeyword
WHERE length(value)<200
  AND uri IN (SELECT url FROM vivo_elastic.person);

CREATE VIEW vivo_elastic.staging_person_research_area AS
SELECT DISTINCT
	uri AS url,
    value AS research_area
FROM vivo_aggregated.person_property_hasresearcharea
WHERE uri IN (SELECT url FROM vivo_elastic.person);

CREATE VIEW vivo_elastic.staging_person_research_overview AS
SELECT DISTINCT ON (URL)
	uri AS url,
    value AS overview
FROM vivo_aggregated.person_property_researchoverview
WHERE uri IN (SELECT url FROM vivo_elastic.person);

CREATE TABLE vivo_elastic.academic_article (
    id integer,
    uri text PRIMARY KEY,
    label text,
    doi text,
    pmid text,
    pmcid text,
    volume text,
    number text,
    issue text,
    start_page text,
    end_page text,
    date_time text,
	FOREIGN KEY (id) REFERENCES vivo_elastic.site(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE vivo_elastic.academic_article_abstract (
    uri text,
    abstract text,
    PRIMARY KEY (uri),
	FOREIGN KEY (uri) REFERENCES vivo_elastic.academic_article(uri) ON UPDATE CASCADE ON DELETE CASCADE    
);

CREATE TABLE vivo_elastic.academic_article_keyword (
    uri text,
    keyword text,
    PRIMARY KEY (uri,keyword),
	FOREIGN KEY (uri) REFERENCES vivo_elastic.academic_article(uri) ON UPDATE CASCADE ON DELETE CASCADE    
);

CREATE TABLE vivo_elastic.academic_article_publication_venue (
    uri text,
    venue text,
    PRIMARY KEY (uri,venue),
	FOREIGN KEY (uri) REFERENCES vivo_elastic.academic_article(uri) ON UPDATE CASCADE ON DELETE CASCADE    
);

CREATE TABLE vivo_elastic.academic_article_subject_area (
    uri text,
    subject_area text,
    PRIMARY KEY (uri,subject_area),
	FOREIGN KEY (uri) REFERENCES vivo_elastic.academic_article(uri) ON UPDATE CASCADE ON DELETE CASCADE    
);

CREATE TABLE vivo_elastic.authorship (
	person_uri text,
	article_uri text,
	rank text,
	corresponding text,
	PRIMARY KEY(person_uri, article_uri),
	FOREIGN KEY (person_uri) REFERENCES vivo_elastic.person(url) ON UPDATE CASCADE ON DELETE CASCADE,
	FOREIGN KEY (article_uri) REFERENCES vivo_elastic.academic_article(uri) ON UPDATE CASCADE ON DELETE CASCADE
);

----------------------

truncate vivo_elastic.site cascade;
insert into vivo_elastic.site select * from vivo_aggregated.site;
insert into vivo_elastic.person select * from vivo_elastic.staging_person;
insert into vivo_elastic.person_keyword select * from vivo_elastic.staging_person_keyword;
insert into vivo_elastic.person_research_area select * from vivo_elastic.staging_person_research_area;
insert into vivo_elastic.person_research_overview select * from vivo_elastic.staging_person_research_overview;
insert into vivo_elastic.academic_article select distinct on (uri) * from vivo_aggregated.academic_article where id in (select id from vivo_elastic.site);
analyze vivo_elastic.academic_article;
analyze vivo_elastic.person;
insert into vivo_elastic.authorship select distinct on (person_uri,article_uri) person_uri,article_uri,rank,corresponding from vivo_aggregated.authorship where person_uri in (select url from vivo_elastic.person) and article_uri in (select uri from vivo_elastic.academic_article);
insert into vivo_elastic.academic_article_abstract select distinct on (uri) * from vivo_aggregated.academic_article_property_abstract where uri in (select uri from vivo_elastic.academic_article);
insert into vivo_elastic.academic_article_keyword select distinct on (uri,value) * from vivo_aggregated.academic_article_property_freetextkeyword where length(value)<200 and uri in (select uri from vivo_elastic.academic_article);
insert into vivo_elastic.academic_article_publication_venue select distinct on (uri,value) * from vivo_aggregated.academic_article_property_haspublicationvenue where length(value)<200 and uri in (select uri from vivo_elastic.academic_article);
insert into vivo_elastic.academic_article_subject_area select distinct on (uri,value) * from vivo_aggregated.academic_article_property_hassubjectarea where length(value)<200 and uri in (select uri from vivo_elastic.academic_article);

