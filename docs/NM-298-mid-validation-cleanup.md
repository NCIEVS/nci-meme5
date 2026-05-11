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
- `Atom current MTH concept terminology id missing`: finds publishable atoms
  without a `conceptTerminologyIds` entry for `MTH` plus the current `MTH`
  version.

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
) q
```

Short CTE names are used only because the workflow query must fit under the
database column limit:

- `ba`: atom ids connected to unpublishable owners
- `bd`: definition ids connected to unpublishable owners
- `bt`: attribute ids connected to unpublishable owners

The cleanup SQL below uses `b'0'` and `b'1'` because `publishable` columns are
stored as `bit(1)`.

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

## Atom Current MTH Concept Terminology Ids

This check identifies publishable atoms that do not have an atom
`conceptTerminologyIds` entry for the current MTH release key. The key is built
from `terminologies` as `MTH` plus the current MTH `version`; the workflow
query does not hard-code the version.

Saved workflow query:

```sql
select a.id componentId, 'ATOM' componentType
from atoms a
join terminologies t on t.terminology = 'MTH' and t.current = true
left join atomjpa_conceptterminologyids cid
  on cid.AtomJpa_id = a.id
  and cid.conceptTerminologyIds_KEY = concat(t.terminology, t.version)
where a.publishable
  and cid.AtomJpa_id is null
```

Review query:

```sql
select
  concat(t.terminology, t.version) requiredKey,
  a.terminology,
  a.version,
  count(*) atomCt
from atoms a
join terminologies t on t.terminology = 'MTH' and t.current = true
left join atomjpa_conceptterminologyids cid
  on cid.AtomJpa_id = a.id
  and cid.conceptTerminologyIds_KEY = concat(t.terminology, t.version)
where a.publishable
  and cid.AtomJpa_id is null
group by requiredKey, a.terminology, a.version
order by atomCt desc, a.terminology, a.version;
```

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
before committing.

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
  and (f.publishable = b'0' or t.publishable = b'0');

update concept_relationships r
join concepts f on r.from_id = f.id
join concepts t on r.to_id = t.id
set r.publishable = b'0',
    r.lastModified = @nm298_time,
    r.lastModifiedBy = @nm298_user
where r.publishable = b'1'
  and (f.publishable = b'0' or t.publishable = b'0');

select row_count() conceptRelationshipsUpdated;
```

### 2. Mappings

```sql
select count(*) mappingCt
from mappings m
join mapsets s on m.mapSet_id = s.id
where m.publishable = b'1'
  and s.publishable = b'0';

update mappings m
join mapsets s on m.mapSet_id = s.id
set m.publishable = b'0',
    m.lastModified = @nm298_time,
    m.lastModifiedBy = @nm298_user
where m.publishable = b'1'
  and s.publishable = b'0';

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

After cleanup, rerun the MID validation check. The expected result is an empty
set.

If the check still returns rows, group by `componentType` first. That keeps the
next cleanup pass focused and avoids chasing individual rows before the failure
shape is clear.
