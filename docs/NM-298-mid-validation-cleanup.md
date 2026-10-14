# NM-298 MID Validation Cleanup

## Summary

Add MID validation checks for publishable content that should not remain
publishable, and provide data-agnostic cleanup SQL for the most likely
server-side failures from the unpublishable-object check.

The workflow checks are defined in:

- `config/src/main/resources/data/SAMPLE_NCI/workflow/workflow.MVO.txt`
- Workflow config type: `MID_VALIDATION_OTHER`
- Result columns: `componentId`, `componentType`

Checks:

- `Publishable old version content`: finds publishable content in non-current
  terminology versions.
- `Publishable content connected to unpublishable object`: finds publishable
  content attached to an unpublishable parent, endpoint, owner, subset, or
  mapset.
- `Mapset target terminology version missing`: finds mapsets whose
  `toTerminology` and `toVersion` do not match a row in `terminologies`.
- `Tree position for non-current terminology version`: finds tree positions
  whose terminology/version is not marked current in `terminologies`.
- `Current MTH atom missing current MTH concept terminology id`: finds
  publishable atoms from the current `MTH` release without a
  `conceptTerminologyIds` entry for that release.

The validation queries intentionally return only two values per row so they
follow the existing MID validation result shape.

## Publishable Old Version Content

This check identifies publishable content in terminology versions where
`terminologies.current = false`. It covers atoms, attributes, atom classes,
relationships, definitions, subsets, mapsets, subset members, and mappings.

The saved workflow query is minified to stay under the
`workflow_bin_definitions.query` length limit. A readable equivalent is:

```sql
select a.id componentId, 'ATOM' componentType
from terminologies t
straight_join atoms a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'ATTRIBUTE'
from terminologies t
straight_join attributes a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'CONCEPT'
from terminologies t
straight_join concepts a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'CODE'
from terminologies t
straight_join codes a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'DESCRIPTOR'
from terminologies t
straight_join descriptors a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'LEXICAL_CLASS'
from terminologies t
straight_join lexical_classes a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'STRING_CLASS'
from terminologies t
straight_join string_classes a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'ATOM_RELATIONSHIP'
from terminologies t
straight_join atom_relationships a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'CONCEPT_RELATIONSHIP'
from terminologies t
straight_join concept_relationships a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'CODE_RELATIONSHIP'
from terminologies t
straight_join code_relationships a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'DESCRIPTOR_RELATIONSHIP'
from terminologies t
straight_join descriptor_relationships a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'COMPONENT_INFO_RELATIONSHIP'
from terminologies t
straight_join component_info_relationships a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'DEFINITION'
from terminologies t
straight_join definitions a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'ATOM_SUBSET'
from terminologies t
straight_join atom_subsets a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'CONCEPT_SUBSET'
from terminologies t
straight_join concept_subsets a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'MAPSET'
from terminologies t
straight_join mapsets a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'ATOM_SUBSET_MEMBER'
from terminologies t
straight_join atom_subset_members a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'CONCEPT_SUBSET_MEMBER'
from terminologies t
straight_join concept_subset_members a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable
union all
select a.id, 'MAPPING'
from terminologies t
straight_join mappings a on a.terminology = t.terminology and a.version = t.version
where t.current = false and a.publishable;
```

If this check returns rows, review the affected `componentType`,
`terminology`, and `version` first. The expected shape is old-version content
that stayed publishable after a newer version became current.

Review query:

```sql
select componentType, terminology, version, count(*) componentCt
from (
  select 'ATOM' componentType, a.terminology, a.version
  from terminologies t
  straight_join atoms a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'ATTRIBUTE', a.terminology, a.version
  from terminologies t
  straight_join attributes a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'CONCEPT', a.terminology, a.version
  from terminologies t
  straight_join concepts a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'CODE', a.terminology, a.version
  from terminologies t
  straight_join codes a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'DESCRIPTOR', a.terminology, a.version
  from terminologies t
  straight_join descriptors a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'LEXICAL_CLASS', a.terminology, a.version
  from terminologies t
  straight_join lexical_classes a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'STRING_CLASS', a.terminology, a.version
  from terminologies t
  straight_join string_classes a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'ATOM_RELATIONSHIP', a.terminology, a.version
  from terminologies t
  straight_join atom_relationships a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'CONCEPT_RELATIONSHIP', a.terminology, a.version
  from terminologies t
  straight_join concept_relationships a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'CODE_RELATIONSHIP', a.terminology, a.version
  from terminologies t
  straight_join code_relationships a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'DESCRIPTOR_RELATIONSHIP', a.terminology, a.version
  from terminologies t
  straight_join descriptor_relationships a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'COMPONENT_INFO_RELATIONSHIP', a.terminology, a.version
  from terminologies t
  straight_join component_info_relationships a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'DEFINITION', a.terminology, a.version
  from terminologies t
  straight_join definitions a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'ATOM_SUBSET', a.terminology, a.version
  from terminologies t
  straight_join atom_subsets a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'CONCEPT_SUBSET', a.terminology, a.version
  from terminologies t
  straight_join concept_subsets a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'MAPSET', a.terminology, a.version
  from terminologies t
  straight_join mapsets a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'ATOM_SUBSET_MEMBER', a.terminology, a.version
  from terminologies t
  straight_join atom_subset_members a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'CONCEPT_SUBSET_MEMBER', a.terminology, a.version
  from terminologies t
  straight_join concept_subset_members a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
  union all
  select 'MAPPING', a.terminology, a.version
  from terminologies t
  straight_join mappings a on a.terminology = t.terminology and a.version = t.version
  where t.current = false and a.publishable
) q
group by componentType, terminology, version
order by componentCt desc, componentType, terminology, version;
```

No blanket cleanup command is included for this check. If it fails on the
server, confirm that the affected terminology/version is intentionally
non-current before making a version-scoped publishability update.

## Connected To Unpublishable Object

This check identifies publishable content attached to unpublishable objects.
It intentionally does not flag mappings under published, non-obsolete PDQ
mapsets that are no longer publishable, because those mapsets represent
historical PDQ mapping targets retained for release history while a newer
publishable mapset is used for current MRMAP/MRSMAP output.
It also does not flag publishable three-character `BR*` concept relationships
when exactly one endpoint is publishable. The unpublishable-to-publishable half
is used to generate MRCUI history for retired CUIs, and the
publishable-to-unpublishable half is its expected stored inverse.

The saved workflow query is minified to stay under the
`workflow_bin_definitions.query` length limit. It also starts with `select` so
that the UI query tester accepts it. A readable equivalent is:

```sql
select *
from (
  with
  ba(id) as (
    select j.atoms_id
    from concepts_atoms j, concepts o
    where j.concepts_id = o.id and o.publishable = 0
    union
    select j.atoms_id
    from codes_atoms j, codes o
    where j.codes_id = o.id and o.publishable = 0
    union
    select j.atoms_id
    from descriptors_atoms j, descriptors o
    where j.descriptors_id = o.id and o.publishable = 0
    union
    select j.atoms_id
    from lexical_classes_atoms j, lexical_classes o
    where j.lexical_classes_id = o.id and o.publishable = 0
    union
    select j.atoms_id
    from string_classes_atoms j, string_classes o
    where j.string_classes_id = o.id and o.publishable = 0
  ),
  bd(id) as (
    select j.definitions_id
    from atoms_definitions j, atoms o
    where j.atoms_id = o.id and o.publishable = 0
    union
    select j.definitions_id
    from concepts_definitions j, concepts o
    where j.concepts_id = o.id and o.publishable = 0
    union
    select j.definitions_id
    from descriptors_definitions j, descriptors o
    where j.descriptors_id = o.id and o.publishable = 0
  ),
  bt(id) as (
    select j.attributes_id
    from atoms_attributes j, atoms o
    where j.atoms_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from concepts_attributes j, concepts o
    where j.concepts_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from codes_attributes j, codes o
    where j.codes_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from descriptors_attributes j, descriptors o
    where j.descriptors_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from atom_relationships_attributes j, atom_relationships o
    where j.atom_relationships_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from concept_relationships_attributes j, concept_relationships o
    where j.concept_relationships_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from code_relationships_attributes j, code_relationships o
    where j.code_relationships_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from descriptor_relationships_attributes j, descriptor_relationships o
    where j.descriptor_relationships_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from definitions_attributes j, definitions o
    where j.definitions_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from atom_subset_members_attributes j, atom_subset_members o
    where j.atom_subset_members_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from concept_subset_members_attributes j, concept_subset_members o
    where j.concept_subset_members_id = o.id and o.publishable = 0
    union
    select j.attributes_id
    from mappings_attributes j, mappings o
    where j.mappings_id = o.id and o.publishable = 0
  )
  select distinct a.id componentId, 'ATOM' componentType
  from atoms a, ba x
  where a.id = x.id and a.publishable
  union all
  select distinct d.id, 'DEF'
  from definitions d, bd x
  where d.id = x.id and d.publishable
  union all
  select distinct a.id, 'ATTR'
  from attributes a, bt x
  where a.id = x.id and a.publishable
  union all
  select r.id, 'A_REL'
  from atom_relationships r, atoms f, atoms t
  where r.from_id = f.id
    and r.to_id = t.id
    and r.publishable
    and (f.publishable = 0 or t.publishable = 0)
  union all
  select r.id, 'C_REL'
  from concept_relationships r, concepts f, concepts t
  where r.from_id = f.id
    and r.to_id = t.id
    and r.publishable
    and (f.publishable = 0 or t.publishable = 0)
    and not (
      r.relationshipType like 'BR_'
      and f.publishable <> t.publishable
    )
  union all
  select r.id, 'CD_REL'
  from code_relationships r, codes f, codes t
  where r.from_id = f.id
    and r.to_id = t.id
    and r.publishable
    and (f.publishable = 0 or t.publishable = 0)
  union all
  select r.id, 'D_REL'
  from descriptor_relationships r, descriptors f, descriptors t
  where r.from_id = f.id
    and r.to_id = t.id
    and r.publishable
    and (f.publishable = 0 or t.publishable = 0)
  union all
  select m.id, 'A_SM'
  from atom_subset_members m, atoms a, atom_subsets s
  where m.member_id = a.id
    and m.subset_id = s.id
    and m.publishable
    and (a.publishable = 0 or s.publishable = 0)
  union all
  select m.id, 'C_SM'
  from concept_subset_members m, concepts c, concept_subsets s
  where m.concept_id = c.id
    and m.subset_id = s.id
    and m.publishable
    and (c.publishable = 0 or s.publishable = 0)
  union all
  select m.id, 'MAPPING'
  from mappings m, mapsets s
  where m.mapSet_id = s.id
    and m.publishable
    and s.publishable = 0
    and not (
      s.terminology = 'PDQ'
      and s.published
      and s.obsolete = 0
    )
) q
```

Short CTE names are used only because the workflow query must fit under the
database column limit:

- `ba`: atom ids connected to unpublishable owners
- `bd`: definition ids connected to unpublishable owners
- `bt`: attribute ids connected to unpublishable owners

The cleanup SQL below uses `b'0'` and `b'1'` because `publishable` columns are
stored as `bit(1)`.

### Preventing New Concept Relationship Cases

When Matrix Initializer, Compute Preferred Names, concept approval, or a
concept-level Query Action changes a concept from publishable to unpublishable,
the connected concept relationships and their attributes are also made
unpublishable. The affected relationship count is written to the algorithm or
molecular-action log.

Relationship Loader does not change incoming data automatically. It reports
the number of publishable non-bequeathal concept relationship input rows that
resolve to an unpublishable endpoint and logs identifying information for the
first ten rows.

### Rollout And Monitoring Plan

Use the following sequence before cleaning historical data:

1. Deploy the revised code and update the existing
   `workflow_bin_definitions` row with the query stored in `workflow.MVO.txt`.
2. Run the revised MID validation check and save the exact failing C_REL ids as
   a baseline. Do not rely only on the total count because removals and
   additions can offset each other.
3. Allow normal editor activity and representative terminology insertions to
   run with the revised code.
4. Compare the current failing C_REL ids with the baseline after each
   monitoring interval or insertion.
5. Investigate every new id using the relationship and endpoint timestamps,
   `lastModifiedBy` values, molecular-action logs, and Relationship Loader
   samples.
6. Run the historical cleanup only after the failing id set remains stable.

Create the baseline after the revised database query and code are in place:

```sql
create table op328_bad_crel_baseline as
select r.id relId
from concept_relationships r
join concepts f on r.from_id = f.id
join concepts t on r.to_id = t.id
where r.publishable = b'1'
  and (f.publishable = b'0' or t.publishable = b'0')
  and not (
    r.relationshipType like 'BR_'
    and f.publishable <> t.publishable
  );

alter table op328_bad_crel_baseline add primary key (relId);
```

Use a ticket- or date-specific table name if more than one baseline must be
retained. Find newly failing relationships with:

```sql
select
  r.id relId,
  r.terminology,
  r.version,
  r.relationshipType,
  r.lastModified relLastModified,
  r.lastModifiedBy relLastModifiedBy,
  r.from_id,
  f.publishable fromPublishable,
  f.lastModified fromLastModified,
  f.lastModifiedBy fromLastModifiedBy,
  r.to_id,
  t.publishable toPublishable,
  t.lastModified toLastModified,
  t.lastModifiedBy toLastModifiedBy
from concept_relationships r
join concepts f on r.from_id = f.id
join concepts t on r.to_id = t.id
left join op328_bad_crel_baseline b on b.relId = r.id
where b.relId is null
  and r.publishable = b'1'
  and (f.publishable = b'0' or t.publishable = b'0')
  and not (
    r.relationshipType like 'BR_'
    and f.publishable <> t.publishable
  )
order by r.id;
```

The containment target is no newly failing C_REL ids after editor activity and
insertions. Existing baseline rows are expected to remain until the historical
cleanup is run.

## Mapset Target Terminology Version

This check identifies mapsets whose `toTerminology` and `toVersion` pair cannot
be found in `terminologies`. Null or blank target values are also failures
because they cannot match a terminology/version row.

Saved workflow query:

```sql
select m.id componentId, 'MAPSET' componentType
from mapsets m
where not exists (
  select 1
  from terminologies t
  where t.terminology = m.toTerminology
    and t.version = m.toVersion
)
```

Review query:

```sql
select
  m.toTerminology,
  m.toVersion,
  count(*) mapsetCt
from mapsets m
where not exists (
  select 1
  from terminologies t
  where t.terminology = m.toTerminology
    and t.version = m.toVersion
)
group by m.toTerminology, m.toVersion
order by mapsetCt desc, m.toTerminology, m.toVersion;
```

## Tree Positions For Current Versions

This check identifies tree positions whose `terminology` and `version` do not
identify a current terminology row. It covers atom, concept, descriptor, and
code tree positions. Rows with missing terminology/version metadata are also
failures because they cannot match a current terminology row.

Saved workflow query:

```sql
select a.id componentId, 'ATOM_TREEPOS' componentType
from atom_tree_positions a
where not exists (
  select 1
  from terminologies t
  where t.terminology = a.terminology
    and t.version = a.version
    and t.current = true
)
union all
select c.id, 'CONCEPT_TREEPOS'
from concept_tree_positions c
where not exists (
  select 1
  from terminologies t
  where t.terminology = c.terminology
    and t.version = c.version
    and t.current = true
)
union all
select d.id, 'DESC_TREEPOS'
from descriptor_tree_positions d
where not exists (
  select 1
  from terminologies t
  where t.terminology = d.terminology
    and t.version = d.version
    and t.current = true
)
union all
select c.id, 'CODE_TREEPOS'
from code_tree_positions c
where not exists (
  select 1
  from terminologies t
  where t.terminology = c.terminology
    and t.version = c.version
    and t.current = true
)
```

Review query:

```sql
select componentType, terminology, version, count(*) treePositionCt
from (
  select 'ATOM_TREEPOS' componentType, a.terminology, a.version
  from atom_tree_positions a
  where not exists (
    select 1
    from terminologies t
    where t.terminology = a.terminology
      and t.version = a.version
      and t.current = true
  )
  union all
  select 'CONCEPT_TREEPOS', c.terminology, c.version
  from concept_tree_positions c
  where not exists (
    select 1
    from terminologies t
    where t.terminology = c.terminology
      and t.version = c.version
      and t.current = true
  )
  union all
  select 'DESC_TREEPOS', d.terminology, d.version
  from descriptor_tree_positions d
  where not exists (
    select 1
    from terminologies t
    where t.terminology = d.terminology
      and t.version = d.version
      and t.current = true
  )
  union all
  select 'CODE_TREEPOS', c.terminology, c.version
  from code_tree_positions c
  where not exists (
    select 1
    from terminologies t
    where t.terminology = c.terminology
      and t.version = c.version
      and t.current = true
  )
) q
group by componentType, terminology, version
order by treePositionCt desc, componentType, terminology, version;
```

## Current MTH Atom Concept Terminology Ids

This check identifies publishable atoms from the current MTH release that do
not have an atom `conceptTerminologyIds` entry for that release. The key is
built from `terminologies` as `MTH` plus the current MTH `version`; the
workflow query does not hard-code the version.

The check is intentionally limited to atoms whose terminology and version are
the current MTH release. Non-MTH atoms are not guaranteed to have the current
MTH key. In particular, SNOMEDCT_US is loaded through a separate insertion and
is skipped while the MTH atom loader processes `umlscui.txt`. Source atoms
inserted after the current MTH release also cannot be expected to have that
release key.

Saved workflow query:

```sql
select a.id componentId, 'ATOM' componentType
from atoms a
join terminologies t
  on t.terminology = 'MTH'
  and t.version = a.version
  and t.current = true
left join atomjpa_conceptterminologyids cid
  on cid.AtomJpa_id = a.id
  and cid.conceptTerminologyIds_KEY = concat(t.terminology, t.version)
where a.terminology = 'MTH'
  and a.publishable
  and cid.AtomJpa_id is null
```

Review query:

```sql
select
  concat(t.terminology, t.version) requiredKey,
  count(*) missingAtomCt,
  min(a.id) sampleAtomId
from atoms a
join terminologies t
  on t.terminology = 'MTH'
  and t.version = a.version
  and t.current = true
left join atomjpa_conceptterminologyids cid
  on cid.AtomJpa_id = a.id
  and cid.conceptTerminologyIds_KEY = concat(t.terminology, t.version)
where a.terminology = 'MTH'
  and a.publishable
  and cid.AtomJpa_id is null
group by requiredKey;
```

## Retired Source Without Bequeathal To Root

The corrected `Retired source w/o bequeathal rel to root` workflow check finds
unpublishable `SRC/VAB` atoms for old versions of sources that still have a
current release. It accepts all three bequeathal types (`BRO`, `BRN`, `BRB`),
matches the exact `V-<terminology>` root code or an approved alternate root,
and requires a publishable root concept and RAB in the same project
terminology/version. A version already in
its own publishable root concept needs no self-relationship. Current sources
with missing or unpublishable roots still fail the check. Entirely retired
sources are outside this check; their historical bequeathals require separate
review. The separate `Publishable Bequeathal Relationship` check enforces
unpublishable relationship flags.

The relationship itself need not be owned by the project terminology/version.
The loader preserves relationship source provenance, so an unpublished
`MTH/2026AA` bequeathal can legitimately connect two `NCIMTH/latest` concepts.
Do not require `r.terminology = c.terminology` or `r.version = c.version`, and
do not duplicate or retag valid MTH-owned links to satisfy the check.

The user confirmed these alternate roots are intentional: CDCREC may bequeath
to CDC, and MTHSPL or MTHCMSFRF may bequeath to MTH. The check accepts each
source's own root and these explicit alternatives, including when the old VAB
is already in an accepted live root concept. Other source/root combinations
still require review; no broader family or prefix-based exception is inferred.

[retired-source-bequeathal-check.sql](retired-source-bequeathal-check.sql) is a
read-only, application-ready query for `NCIMTH/latest`, with literal values and
no variables or trailing semicolon. Paste its contents into the bin's Query
field. The saved definition in `workflow.MVO.txt` uses the equivalent project
parameters for reuse. Update the existing bin's Query and Description through
the workflow editor; merely deploying this file does not update a database's
existing bin. Add a semicolon when executing the SQL interactively in MySQL.

The corrected query returns `clusterId, conceptId`, using the same concept id
in both columns to create one independent cluster per finding. This also keeps
the two-column result shape required by the MID report. Bin regeneration always
uses `executeClusteredConceptQuery`, which treats `conceptId1, conceptId2` as
pairs to cluster and discards pairs whose ids are equal. Thus the original
query can return 30 rows in SQL or the MID report while generating an empty bin.
Whitespace is not the cause. To regenerate using the original predicates,
change only its SELECT list to
`SELECT DISTINCT c_a.concepts_id AS clusterId, c_a.concepts_id AS conceptId`.
The bin editor's Test endpoint has a separate issue: for query style `OTHER`,
it executes a limited query but discards the results and returns a zero count.
That Test behavior does not explain regeneration; the duplicate-id pair
handling does.

The original 30 findings were resolved diagnostically as follows:

| Findings | Evidence | Repair |
| --- | --- | --- |
| 15 old concepts | Existing `BRN` to the correct root and inverse `BRB` | Preserve those relationships; the old check accepted only `BRO` |
| 12 old concepts | CCS_10, NDFRT, NDFRT_FDASPL, NDFRT_FMTSME have no current release and unpublished root RABs | Exclude these fully retired sources from the root check |
| 3 old concepts | Existing `BRO` goes elsewhere, with no relationship to the expected root | Add the missing root pairs below |

The earlier diagnostic also showed 24 existing `BRN`/`BRB` rows marked
publishable. That concerns the separate `Publishable Bequeathal Relationship`
check and is not a prerequisite for clearing this missing-root check.
Publishability cleanup is covered separately below.
NDFRT's root concept remains publishable but its RAB does not; that is not a
reason to republish the RAB or change the concept. These results describe the
supplied snapshot, not which insertion or editor caused it.

### Add The Three Missing Root Pairs

| Source | Old concept / CUI | Root concept / CUI | Existing destination retained |
| --- | --- | --- | --- |
| ICD10 | 2859814 / CL466586 | 28704768 / CL1793137 | 658023 / C1137110 (publishable, no SRC RAB) |
| RXNORM | 43277739 / C5978422 | 660568 / C1140284 | 38122176 / C5884345 (unpublishable old version) |
| VANDF | 43277741 / C5978423 | 660569 / C1140288 | 38122178 / C5884346 (unpublishable old version) |

Use the scoped `ADHOC` maintenance action added for this repair. After
deploying a build containing the change, configure a maintenance
process for project `NCIMTH/latest` with one enabled `ADHOC` step:

| Parameter | Value |
| --- | --- |
| Action Name | `Add missing retired source root bequeathals (NM-298)` |
| Integer parameter | Unused; leave the default |
| String parameter | Unused; leave blank |

This action requires no input file or input directory. Use the normal process
log configuration. It checks the exact concept ids, CUIs, project versions,
publishability, and retired VAB/live RAB codes for all three pairs before
adding any relationships. It locks the six concepts in id order and adds the
missing `BRO` links with metadata-defined inverses through the content service.
Both directions are unpublishable, unpublished, and `READY_FOR_PUBLICATION`.
All additions share one transaction, with rollback on failure. The normal
persistence path allocates ids and updates search state, so no separate manual
reindex is needed.

Expect the log to report `Root pairs added = 3; already valid = 0; relationship
rows added = 6`. A rerun with three complete, unpublishable bequeathal pairs
reports zero added and three already valid. Existing valid `BRN`/`BRB` pairs
and MTH-owned pairs are accepted without alteration. A partial, duplicate,
conflicting, or publishable pair causes the action to stop for review, as does
an endpoint or SRC atom that no longer matches the supplied snapshot. Existing
links to other destinations and concept publishability remain unchanged.

Validation: nine database-free `RetiredSourceRootRepairUnitTest` tests pass,
covering the three exact pairs, endpoint/atom drift, reruns, preserved history,
and conflicting or incomplete pairs. Main/test Checkstyle and SpotBugs also
pass. The action has not been deployed or run against the target database.

After running the AdHoc action, rerun the updated root check and regenerate
its bin. Expect zero findings on the reviewed data snapshot. Verify the six
new rows have the intended endpoints, inverses, and false publishability
flags. No separate SQL data repair is needed for this check.

The repository changes prepare the repair; no target-database changes have
been executed by the agent.

### Additional Findings From The Corrected Check

A full-database run of an earlier revision of the corrected check returned 22
concepts: the three known cases and 19 outside the original report. Follow-up
inspection found 16 of the additional concepts already have unpublished
`MTH/2026AA` BROs to their correct, publishable source roots. The relationship
ownership restriction in that revision was too strict and has been removed.
In reconstructed test data, that correction reduced the exact-root check from
22 concepts to six. The user then confirmed the alternate destinations for the
other three additional concepts are intentional:

| Concept | Source | Existing destination | Review |
| --- | --- | --- | --- |
| 25270005 / CL1677269 | CDCREC | V-CDC (2832454 / CL337097) | Approved alternate root; preserve the relationship |
| 40541569 / C5978416 | MTHCMSFRF | V-MTH (105671 / C0220961) | Approved alternate root; preserve the relationship and publishable concept |
| 43277743 / C5978424 | MTHSPL | V-MTH (105671 / C0220961) | Approved alternate root; preserve the relationship |

Concept 46009702 has two BROs: one NCIMTH-owned to V-MTH and one MTH-owned to
its exact V-MTHSPL root. The latter already satisfies the revised check. The
25 diagnostic rows represent 22 concepts because 40541569 has three matching
VAB codes and 46009702 has two relationships. With the approved alternatives
included, the final check leaves only the three original missing-root cases
on the supplied snapshot. Keep the AdHoc repair limited to those three pairs.
Matching own or approved alternate roots is an additional requirement relative
to the original check, which accepted any live SRC RAB.

The user subsequently executed the final query in MySQL and confirmed exactly
three rows in 0.17 seconds: ICD10 `2859814`, RXNORM `43277739`, and VANDF
`43277741`. Each row has the same id in `clusterId` and `conceptId`. This
confirms the final database result matches the three AdHoc repair targets;
it does not establish that the repair has run or that bin regeneration has
been verified. After successfully adding these root pairs, the check is
expected to return zero on this data snapshot.

The application bin count remains a separate verification: save the exact
revised query, regenerate, and compare it with the raw SQL result.

## Publishable Bequeathal Relationships

The supplied `NCIMTH` check returned 64 publishable relationship rows, or 32
forward/inverse pairs. All had `lastModifiedBy = E-CFC` and workflow status
`READY_FOR_PUBLICATION`, with creation timestamps on August 24–25 and
September 25, 2026. That pattern is consistent with editor creation followed
by approval; the last modifier alone does not prove which path created each
row. Creation atomic actions are needed for definitive attribution.

The editor's single and bulk Add Relationship endpoints previously set every
non-XR relationship to publishable, including `BRO`, `BRN`, and `BRB`. Inverse
creation copied that flag. `CreateConceptRelBequeathalAlgorithm` independently
set new bequeathals to publishable, and relationship loaders honored the input
publishability flag. Approval did not consistently clear it for relationships
on concepts that were already retired. This allowed new findings to accumulate
after a cleanup.

The permanent fix enforces `publishable = false` for concept-level `BRO`,
`BRN`, and `BRB` relationships, including inverses and source-owned rows:

- Editor defaults and the automatic bequeathal creator now produce
  unpublishable bequeathals. The loader normalizes flags after mapping legacy
  relationship types and before constructing inverses.
- Shared component add/update paths enforce the rule before persistence and
  atomic-action comparison. JPA pre-persist/pre-update callbacks also cover
  direct changes to managed entities that bypass an explicit service update.
- The historical `published` flag, workflow status, endpoints, and other
  relationship types are preserved. Existing invalid rows remain visible when
  read; this change is not a bulk data migration.

After deploying the fix, use the existing `ADHOC` action **Fix bequeathal rels
to unpublishable** once to clear existing findings. Its query covers all
terminologies and versions, so its update count can exceed the 64 rows from
the `NCIMTH` check. It changes the relationship publishable flag through the
content service and normal search indexing; no separate manual reindex or new
SQL repair file is needed. Use the usual AdHoc process configuration, including
its existing input-directory requirement. This cleanup is independent of the
three missing-root pairs above.

Rerun the existing validation query after cleanup:

```sql
SELECT r.from_id AS conceptId1, r.to_id AS conceptId2
FROM concept_relationships r
WHERE r.terminology = 'NCIMTH'
  AND r.relationshipType IN ('BRO', 'BRB', 'BRN')
  AND r.publishable = true;
```

Expect zero rows after successful cleanup. On a test instance, add a BRO and
a BRN through the editor, approve their concepts, and verify both directions
remain unpublishable. The database-free `RelationshipPublishabilityUnitTest`
covers the shared rule, creation flag order, inverse pairs, JPA callback wiring,
attempts to republish, and preservation of unrelated fields and components.
Live editor/loader execution and target-database cleanup are separate deployment
verification steps.

Validation: `make quality UNIT_TEST_PATTERN='*Relationship*UnitTest'` passed
main/test Checkstyle and SpotBugs plus 96 unit tests, including the nine new
publishability regressions. No target-database changes were executed.

## Pre-Cleanup Review

Run review queries first on the target server. Counts from local development
databases should not be used for production cleanup decisions.

### Concept Relationship Summary

```sql
select
  r.terminology,
  r.version,
  r.relationshipType,
  r.additionalRelationshipType,
  r.workflowStatus,
  count(*) relCt,
  sum(f.publishable = b'0' and t.publishable = b'1') fromUnpublishableOnly,
  sum(f.publishable = b'1' and t.publishable = b'0') toUnpublishableOnly,
  sum(f.publishable = b'0' and t.publishable = b'0') bothUnpublishable
from concept_relationships r
join concepts f on r.from_id = f.id
join concepts t on r.to_id = t.id
where r.publishable = b'1'
  and (f.publishable = b'0' or t.publishable = b'0')
  and not (
    r.relationshipType like 'BR_'
    and f.publishable <> t.publishable
  )
group by
  r.terminology,
  r.version,
  r.relationshipType,
  r.additionalRelationshipType,
  r.workflowStatus
order by relCt desc;
```

### Mapping Summary

```sql
select
  s.id mapSetId,
  s.terminologyId,
  s.name,
  s.terminology,
  s.version,
  s.fromTerminology,
  s.fromVersion,
  s.toTerminology,
  s.toVersion,
  count(*) mappingCt
from mappings m
join mapsets s on m.mapSet_id = s.id
where m.publishable = b'1'
  and s.publishable = b'0'
  and not (
    s.terminology = 'PDQ'
    and s.published = b'1'
    and s.obsolete = b'0'
  )
group by
  s.id,
  s.terminologyId,
  s.name,
  s.terminology,
  s.version,
  s.fromTerminology,
  s.fromVersion,
  s.toTerminology,
  s.toVersion
order by mappingCt desc;
```

### Atom Summary

```sql
select 'CONCEPT' ownerType, count(distinct a.id) atomCt
from atoms a
join concepts_atoms j on a.id = j.atoms_id
join concepts o on j.concepts_id = o.id
where a.publishable = b'1' and o.publishable = b'0'
union all
select 'CODE', count(distinct a.id)
from atoms a
join codes_atoms j on a.id = j.atoms_id
join codes o on j.codes_id = o.id
where a.publishable = b'1' and o.publishable = b'0'
union all
select 'DESCRIPTOR', count(distinct a.id)
from atoms a
join descriptors_atoms j on a.id = j.atoms_id
join descriptors o on j.descriptors_id = o.id
where a.publishable = b'1' and o.publishable = b'0'
union all
select 'LEXICAL_CLASS', count(distinct a.id)
from atoms a
join lexical_classes_atoms j on a.id = j.atoms_id
join lexical_classes o on j.lexical_classes_id = o.id
where a.publishable = b'1' and o.publishable = b'0'
union all
select 'STRING_CLASS', count(distinct a.id)
from atoms a
join string_classes_atoms j on a.id = j.atoms_id
join string_classes o on j.string_classes_id = o.id
where a.publishable = b'1' and o.publishable = b'0';
```

### Attribute Summary

```sql
select ownerType, count(distinct attrId) attrCt
from (
  select 'ATOM' ownerType, j.attributes_id attrId
  from atoms_attributes j
  join atoms o on j.atoms_id = o.id
  where o.publishable = b'0'
  union all
  select 'CONCEPT', j.attributes_id
  from concepts_attributes j
  join concepts o on j.concepts_id = o.id
  where o.publishable = b'0'
  union all
  select 'CODE', j.attributes_id
  from codes_attributes j
  join codes o on j.codes_id = o.id
  where o.publishable = b'0'
  union all
  select 'DESCRIPTOR', j.attributes_id
  from descriptors_attributes j
  join descriptors o on j.descriptors_id = o.id
  where o.publishable = b'0'
  union all
  select 'ATOM_REL', j.attributes_id
  from atom_relationships_attributes j
  join atom_relationships o on j.atom_relationships_id = o.id
  where o.publishable = b'0'
  union all
  select 'CONCEPT_REL', j.attributes_id
  from concept_relationships_attributes j
  join concept_relationships o on j.concept_relationships_id = o.id
  where o.publishable = b'0'
  union all
  select 'CODE_REL', j.attributes_id
  from code_relationships_attributes j
  join code_relationships o on j.code_relationships_id = o.id
  where o.publishable = b'0'
  union all
  select 'DESC_REL', j.attributes_id
  from descriptor_relationships_attributes j
  join descriptor_relationships o on j.descriptor_relationships_id = o.id
  where o.publishable = b'0'
  union all
  select 'DEF', j.attributes_id
  from definitions_attributes j
  join definitions o on j.definitions_id = o.id
  where o.publishable = b'0'
  union all
  select 'ATOM_SM', j.attributes_id
  from atom_subset_members_attributes j
  join atom_subset_members o on j.atom_subset_members_id = o.id
  where o.publishable = b'0'
  union all
  select 'CONCEPT_SM', j.attributes_id
  from concept_subset_members_attributes j
  join concept_subset_members o on j.concept_subset_members_id = o.id
  where o.publishable = b'0'
  union all
  select 'MAPPING', j.attributes_id
  from mappings_attributes j
  join mappings o on j.mappings_id = o.id
  where o.publishable = b'0'
) x
join attributes a on a.id = x.attrId
where a.publishable = b'1'
group by ownerType
order by attrCt desc;
```

## Cleanup SQL

Run cleanup in a transaction during a maintenance window. Review row counts
before committing. The concept relationship and mapping cleanup predicates
must retain the same valid BR and historical PDQ exclusions used by the MID
validation query.

Recommended order:

1. Concept relationships
2. Mappings
3. Atoms
4. Attributes

Attributes should run last because the first three cleanup steps may create new
publishable-attribute-to-unpublishable-owner cases.

```sql
set @nm298_user = 'NM-298 cleanup';
set @nm298_time = now();

start transaction;
```

### 1. Concept Relationships

```sql
select count(*) conceptRelationshipCt
from concept_relationships r
join concepts f on r.from_id = f.id
join concepts t on r.to_id = t.id
where r.publishable = b'1'
  and (f.publishable = b'0' or t.publishable = b'0')
  and not (
    r.relationshipType like 'BR_'
    and f.publishable <> t.publishable
  );

update concept_relationships r
join concepts f on r.from_id = f.id
join concepts t on r.to_id = t.id
set r.publishable = b'0',
    r.lastModified = @nm298_time,
    r.lastModifiedBy = @nm298_user
where r.publishable = b'1'
  and (f.publishable = b'0' or t.publishable = b'0')
  and not (
    r.relationshipType like 'BR_'
    and f.publishable <> t.publishable
  );

select row_count() conceptRelationshipsUpdated;
```

### 2. Mappings

```sql
select count(*) mappingCt
from mappings m
join mapsets s on m.mapSet_id = s.id
where m.publishable = b'1'
  and s.publishable = b'0'
  and not (
    s.terminology = 'PDQ'
    and s.published = b'1'
    and s.obsolete = b'0'
  );

update mappings m
join mapsets s on m.mapSet_id = s.id
set m.publishable = b'0',
    m.lastModified = @nm298_time,
    m.lastModifiedBy = @nm298_user
where m.publishable = b'1'
  and s.publishable = b'0'
  and not (
    s.terminology = 'PDQ'
    and s.published = b'1'
    and s.obsolete = b'0'
  );

select row_count() mappingsUpdated;
```

### 3. Atoms

```sql
select count(distinct a.id) atomCt
from atoms a
join (
  select j.atoms_id id
  from concepts_atoms j
  join concepts o on j.concepts_id = o.id
  where o.publishable = b'0'
  union
  select j.atoms_id
  from codes_atoms j
  join codes o on j.codes_id = o.id
  where o.publishable = b'0'
  union
  select j.atoms_id
  from descriptors_atoms j
  join descriptors o on j.descriptors_id = o.id
  where o.publishable = b'0'
  union
  select j.atoms_id
  from lexical_classes_atoms j
  join lexical_classes o on j.lexical_classes_id = o.id
  where o.publishable = b'0'
  union
  select j.atoms_id
  from string_classes_atoms j
  join string_classes o on j.string_classes_id = o.id
  where o.publishable = b'0'
) x on a.id = x.id
where a.publishable = b'1';

update atoms a
join (
  select j.atoms_id id
  from concepts_atoms j
  join concepts o on j.concepts_id = o.id
  where o.publishable = b'0'
  union
  select j.atoms_id
  from codes_atoms j
  join codes o on j.codes_id = o.id
  where o.publishable = b'0'
  union
  select j.atoms_id
  from descriptors_atoms j
  join descriptors o on j.descriptors_id = o.id
  where o.publishable = b'0'
  union
  select j.atoms_id
  from lexical_classes_atoms j
  join lexical_classes o on j.lexical_classes_id = o.id
  where o.publishable = b'0'
  union
  select j.atoms_id
  from string_classes_atoms j
  join string_classes o on j.string_classes_id = o.id
  where o.publishable = b'0'
) x on a.id = x.id
set a.publishable = b'0',
    a.lastModified = @nm298_time,
    a.lastModifiedBy = @nm298_user
where a.publishable = b'1';

select row_count() atomsUpdated;
```

### 4. Attributes

```sql
select count(distinct a.id) attributeCt
from attributes a
join (
  select j.attributes_id id
  from atoms_attributes j
  join atoms o on j.atoms_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from concepts_attributes j
  join concepts o on j.concepts_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from codes_attributes j
  join codes o on j.codes_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from descriptors_attributes j
  join descriptors o on j.descriptors_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from atom_relationships_attributes j
  join atom_relationships o on j.atom_relationships_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from concept_relationships_attributes j
  join concept_relationships o on j.concept_relationships_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from code_relationships_attributes j
  join code_relationships o on j.code_relationships_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from descriptor_relationships_attributes j
  join descriptor_relationships o on j.descriptor_relationships_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from definitions_attributes j
  join definitions o on j.definitions_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from atom_subset_members_attributes j
  join atom_subset_members o on j.atom_subset_members_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from concept_subset_members_attributes j
  join concept_subset_members o on j.concept_subset_members_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from mappings_attributes j
  join mappings o on j.mappings_id = o.id
  where o.publishable = b'0'
) x on a.id = x.id
where a.publishable = b'1';

update attributes a
join (
  select j.attributes_id id
  from atoms_attributes j
  join atoms o on j.atoms_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from concepts_attributes j
  join concepts o on j.concepts_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from codes_attributes j
  join codes o on j.codes_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from descriptors_attributes j
  join descriptors o on j.descriptors_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from atom_relationships_attributes j
  join atom_relationships o on j.atom_relationships_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from concept_relationships_attributes j
  join concept_relationships o on j.concept_relationships_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from code_relationships_attributes j
  join code_relationships o on j.code_relationships_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from descriptor_relationships_attributes j
  join descriptor_relationships o on j.descriptor_relationships_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from definitions_attributes j
  join definitions o on j.definitions_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from atom_subset_members_attributes j
  join atom_subset_members o on j.atom_subset_members_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from concept_subset_members_attributes j
  join concept_subset_members o on j.concept_subset_members_id = o.id
  where o.publishable = b'0'
  union
  select j.attributes_id
  from mappings_attributes j
  join mappings o on j.mappings_id = o.id
  where o.publishable = b'0'
) x on a.id = x.id
set a.publishable = b'0',
    a.lastModified = @nm298_time,
    a.lastModifiedBy = @nm298_user
where a.publishable = b'1';

select row_count() attributesUpdated;
```

After reviewing the row counts:

```sql
commit;
-- or rollback;
```

## Post-Cleanup Validation

During the monitoring period, rerun the C_REL baseline comparison after editor
activity and each representative insertion. The expected result is no new ids;
baseline rows remain until cleanup.

After cleanup, rerun the complete MID validation check. The expected result is
an empty set after applying the documented BR and PDQ exclusions.

If the check still returns rows, group by `componentType` first. That keeps the
next cleanup pass focused and avoids chasing individual rows before the failure
shape is clear.
