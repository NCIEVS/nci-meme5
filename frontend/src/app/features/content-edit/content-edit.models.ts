export type ContentComponentType =
  | 'ATOM'
  | 'CODE'
  | 'CONCEPT'
  | 'DESCRIPTOR'
  | 'LUI'
  | 'MAPSET'
  | 'SUI';

export interface ContentPfsParameter {
  ascending: boolean;
  maxResults: number;
  queryRestriction?: string;
  sortField?: string;
  startIndex: number;
}

export interface ContentListState<T> {
  items: T[];
  totalCount: number;
}

export interface ContentListResponse<T> {
  atoms?: T[];
  codes?: T[];
  concepts?: T[];
  descriptors?: T[];
  mappings?: T[];
  mapsets?: T[];
  objects?: T[];
  relationships?: T[];
  results?: T[];
  strings?: T[];
  trees?: T[];
  treePositions?: T[];
  totalCount?: number;
}

export interface ContentStringListResponse {
  objects?: string[];
  strings?: string[];
  totalCount?: number;
}

export interface ContentComponentSummary {
  branch?: string | null;
  id?: number | null;
  lastModified?: string | number | null;
  lastModifiedBy?: string | null;
  name?: string | null;
  obsolete?: boolean | null;
  publishable?: boolean | null;
  published?: boolean | null;
  suppressible?: boolean | null;
  terminology?: string | null;
  terminologyId?: string | null;
  type?: ContentComponentType | string | null;
  version?: string | null;
  workflowStatus?: string | null;
}

export interface ContentSearchResult extends ContentComponentSummary {
  property?: {
    key?: string | null;
    value?: string | null;
  } | null;
  score?: number | null;
  value?: string | null;
  workflowStatus?: string | null;
}

export interface ContentTerminology {
  current?: boolean | null;
  organizingClassType?: ContentComponentType | string | null;
  terminology?: string | null;
  version?: string | null;
}

export interface ContentTerminologyListResponse {
  objects?: ContentTerminology[];
  terminologies?: ContentTerminology[];
  totalCount?: number;
}

export interface ContentAtom extends ContentComponentSummary {
  attributes?: ContentAttribute[];
  codeId?: string | null;
  conceptId?: string | null;
  definitions?: ContentDefinition[];
  descriptorId?: string | null;
  language?: string | null;
  lexicalClassId?: string | null;
  relationships?: ContentRelationship[];
  stringClassId?: string | null;
  termType?: string | null;
}

export interface ContentAttribute {
  id?: number | null;
  name?: string | null;
  terminology?: string | null;
  value?: string | null;
}

export interface ContentDefinition {
  atomElement?: boolean;
  atomElementStr?: string | null;
  id?: number | null;
  obsolete?: boolean | null;
  suppressible?: boolean | null;
  terminology?: string | null;
  value?: string | null;
}

export interface ContentRelationship {
  additionalRelationshipType?: string | null;
  assertedDirection?: boolean | null;
  from?: ContentComponentSummary | null;
  fromId?: number | null;
  fromName?: string | null;
  fromTerminology?: string | null;
  fromTerminologyId?: string | null;
  fromVersion?: string | null;
  group?: string | number | null;
  hierarchical?: boolean | null;
  id?: number | null;
  inferred?: boolean | null;
  lastModifiedBy?: string | null;
  name?: string | null;
  obsolete?: boolean | null;
  published?: boolean | null;
  publishable?: boolean | null;
  relationshipType?: string | null;
  stated?: boolean | null;
  suppressible?: boolean | null;
  terminologyId?: string | null;
  terminology?: string | null;
  to?: ContentComponentSummary | null;
  toId?: number | null;
  toName?: string | null;
  toTerminology?: string | null;
  toTerminologyId?: string | null;
  toVersion?: string | null;
  type?: string | null;
  version?: string | null;
  workflowStatus?: string | null;
}

export interface ContentSemanticType {
  id?: number | null;
  lastModifiedBy?: string | null;
  semanticType?: string | null;
  workflowStatus?: string | null;
}

export interface ContentSemanticTypeMetadata {
  abbreviation?: string | null;
  expandedForm?: string | null;
  terminology?: string | null;
  treeNumber?: string | null;
  typeId?: string | null;
  version?: string | null;
}

export interface ContentSemanticTypeListResponse {
  objects?: ContentSemanticTypeMetadata[];
  totalCount?: number;
  types?: ContentSemanticTypeMetadata[];
}

export interface ContentMapping {
  advice?: string | null;
  group?: string | number | null;
  id?: number | null;
  mapSetId?: number | null;
  obsolete?: boolean | null;
  rank?: string | number | null;
  relationshipType?: string | null;
  rule?: string | null;
  toIdType?: string | null;
  toName?: string | null;
  toTerminologyId?: string | null;
}

export interface ContentNote {
  id?: number | null;
  lastModified?: string | null;
  lastModifiedBy?: string | null;
  note?: string | null;
  timestamp?: string | number | null;
}

export interface ContentSubsetMember {
  id?: number | null;
  member?: ContentComponentSummary | null;
  obsolete?: boolean | null;
  subset?: {
    id?: number | null;
    name?: string | null;
    terminologyId?: string | null;
  } | null;
  suppressible?: boolean | null;
}

export interface ContentTree {
  ancestorPath?: string | null;
  childCt?: number | null;
  children?: ContentTree[];
  nodeId?: number | null;
  nodeName?: string | null;
  nodeTerminology?: string | null;
  nodeTerminologyId?: string | null;
  nodeVersion?: string | null;
  terminology?: string | null;
  type?: ContentComponentType | string | null;
  version?: string | null;
}

export type ContentTreePosition = ContentTree;

export interface ContentComponent extends ContentComponentSummary {
  atoms?: ContentAtom[];
  attributes?: ContentAttribute[];
  definitions?: ContentDefinition[];
  members?: ContentSubsetMember[];
  notes?: ContentNote[];
  relationships?: ContentRelationship[];
  semanticTypes?: ContentSemanticType[];
}

export interface ContentKeyValuePair {
  key?: string | null;
  value?: string | null;
}

export interface ContentKeyValuePairList {
  keyValuePairs?: ContentKeyValuePair[];
  name?: string | null;
}

export interface ContentKeyValuePairLists {
  count?: number;
  keyValuePairLists?: ContentKeyValuePairList[];
}

export interface ContentMetadata {
  additionalRelationshipTypes: ContentKeyValuePair[];
  attributeNames: ContentKeyValuePair[];
  relationshipTypes: ContentKeyValuePair[];
  termTypes: ContentKeyValuePair[];
}

export interface ContentPrecedenceList {
  branch?: string | null;
  id?: number | null;
  lastModified?: string | number | null;
  lastModifiedBy?: string | null;
  name?: string | null;
  precedence?: ContentKeyValuePairList | null;
  terminology?: string | null;
  timestamp?: string | number | null;
  version?: string | null;
}

export interface ContentTermTypeDetail {
  abbreviation?: string | null;
  codeVariantType?: string | null;
  exclude?: boolean | null;
  expandedForm?: string | null;
  hierarchicalType?: boolean | null;
  nameVariantType?: string | null;
  normExclude?: boolean | null;
  obsolete?: boolean | null;
  style?: string | null;
  suppressible?: boolean | null;
  terminology?: string | null;
  usageType?: string | null;
  version?: string | null;
}

export interface ContentRelationshipTypeDetail {
  abbreviation?: string | null;
  expandedForm?: string | null;
  inverseAbbreviation?: string | null;
  terminology?: string | null;
  version?: string | null;
}

export interface MolecularAction {
  id?: number | null;
  componentId?: number | null;
  componentId2?: number | null;
  name?: string | null;
  timestamp?: string | null;
  lastModifiedBy?: string | null;
  undoneFlag?: boolean | null;
  activityId?: number | null;
}

export interface MolecularActionListResponse {
  actions?: MolecularAction[] | null;
  totalCount?: number | null;
}

export interface ContentRouteMode {
  activityId?: string | null;
  componentId?: string | null;
  mode: string;
  projectId?: string | null;
  terminology?: string | null;
  terminologyId?: string | null;
  type?: ContentComponentType | string | null;
  version?: string | null;
}
