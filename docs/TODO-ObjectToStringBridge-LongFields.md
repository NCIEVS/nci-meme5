# TODO: Add ObjectToStringBridge to Long ID Fields

## Background
Long fields with `@GenericField(searchable = Searchable.YES)` need `ObjectToStringBridge` for Lucene queries to work properly in Hibernate Search 7.x.

## Already Fixed
- `AbstractComponent.getId()` - primary id field for content entities
- `AbstractChecklist.getProjectId()` - derived field for worklists/checklists

## Primary ID Fields to Fix (12 files)

| File | Field | Line |
|------|-------|------|
| `UserJpa.java` | `id` | 66 |
| `ProjectJpa.java` | `id` | 81 |
| `SourceDataFileJpa.java` | `id` | 49 |
| `SourceIdRangeJpa.java` | `id` | 46 |
| `TrackingRecordJpa.java` | `id` | 69 |
| `TypeKeyValueJpa.java` | `id` | 46 |
| `ReportJpa.java` | `id` (getter at 151) | 151 |
| `SourceDataJpa.java` | `id` (getter at 199) | 199 |
| `AtomIdentityJpa.java` | `id` | 45 |
| `LexicalClassIdentityJpa.java` | `id` | 38 |
| `RelationshipIdentityJpa.java` | `id` | 42 |
| `SemanticTypeComponentIdentityJpa.java` | `id` | 38 |
| `StringClassIdentityJpa.java` | `id` | 39 |
| `AttributeIdentityJpa.java` | `id` | 42 |

## Foreign Key / Derived ID Fields to Fix

### ProjectId fields
- `TrackingRecordJpa.getProjectId()` - line 322
- `WorkflowBinJpa.getProjectId()` - line 453
- `WorkflowEpochJpa.getProjectId()` - line 209
- `WorkflowConfigJpa.getProjectId()` - line 294
- `AbstractAlgorithmInfo.getProjectId()` - line 219
- `AbstractProcessInfo.getProjectId()` - line 238
- `ReportJpa.getProjectId()` - line 296
- `LogEntryJpa.projectId` - line 67

### Relationship fromId/toId
- `ConceptRelationshipJpa.getFromId()` - line 167
- `ConceptRelationshipJpa.getToId()` - line 296
- `AtomRelationshipJpa.getFromId()` - line 159
- `AtomRelationshipJpa.getToId()` - line 236

### Note entity IDs
- `AtomNoteJpa.getAtomId()` - line 82
- `ConceptNoteJpa.getConceptId()` - line 82
- `DescriptorNoteJpa.getDescriptorId()` - line 82
- `CodeNoteJpa.getCodeId()` - line 85
- `WorklistNoteJpa.getWorklistId()` - line 83
- `ChecklistNoteJpa.getChecklistId()` - line 83

### SubsetMember IDs
- `AtomSubsetMemberJpa.getMemberId()` - line 140
- `AtomSubsetMemberJpa.getSubsetId()` - line 269
- `ConceptSubsetMemberJpa.getMemberId()` - line 142
- `ConceptSubsetMemberJpa.getSubsetId()` - line 271

### TreePosition nodeId
- `ConceptTreePositionJpa.getNodeId()` - line 133
- `AtomTreePositionJpa.getNodeId()` - line 131
- `CodeTreePositionJpa.getNodeId()` - line 127
- `DescriptorTreePositionJpa.getNodeId()` - line 127

### Other fields
- `ProcessExecutionJpa.processConfigId` - line 72
- `AlgorithmExecutionJpa.algorithmConfigId` - line 68
- `AlgorithmExecutionJpa.getProcessId()` - line 183
- `MappingJpa.getMapSetId()` - line 432
- `AtomicActionJpa.objectId` - line 56
- `AtomicActionJpa.getMolecularActionId()` - line 154
- `MolecularActionJpa.componentId` - line 61
- `MolecularActionJpa.componentId2` - line 66
- `ReportJpa.getReport1Id()` - line 241
- `ReportJpa.getReport2Id()` - line 253
- `LogEntryJpa.objectId` - line 62
- `WorkflowBinDefinitionJpa.getWorkflowConfigId()` - line 301

## Fix Pattern

Change from:
```java
@GenericField(searchable = Searchable.YES)
private Long id;
```

To:
```java
@GenericField(searchable = Searchable.YES,
    valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
private Long id;
```

For derived fields on getters:
```java
@GenericField(searchable = Searchable.YES,
    valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
@IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "project")))
public Long getProjectId() {
```

## Required Imports

```java


```
