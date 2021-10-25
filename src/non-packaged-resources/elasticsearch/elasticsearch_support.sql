select distinct
	overall_official_name,
	overall_official_affiliation,
	(select jsonb_pretty(jsonb_agg(foo))
	 from 
	 	(select overall_official_role, nct_id, brief_title from overall_official as bar2 natural join study
	 	 where bar.overall_official_name=bar2.overall_official_name
	 	   and bar.overall_official_affiliation = bar2.overall_official_affiliation
	 	   ) as foo
	)
from clinical_trials.overall_official as bar
limit 10
;
