export interface TerminologyListResponse {
  terminologies?: Terminology[];
  objects?: Terminology[];
  totalCount?: number;
}

export interface Terminology {
  id?: number;
  terminology: string;
  version: string;
  preferredName?: string | null;
  organizingClassType?: string | null;
  citation?: Citation | null;
  current?: boolean;
  metathesaurus?: boolean;
  relatedTerminologies?: string[] | null;
  rootTerminology?: RootTerminology | null;
}

export interface RootTerminology {
  id?: number;
  terminology?: string | null;
  family?: string | null;
  restrictionLevel?: number | null;
  language?: string | null;
  preferredName?: string | null;
  shortName?: string | null;
  hierarchicalName?: string | null;
  acquisitionContact?: ContactInfo | null;
  contentContact?: ContactInfo | null;
  licenseContact?: ContactInfo | null;
}

export interface ContactInfo {
  address1?: string | null;
  address2?: string | null;
  city?: string | null;
  country?: string | null;
  email?: string | null;
  fax?: string | null;
  name?: string | null;
  organization?: string | null;
  stateOrProvince?: string | null;
  telephone?: string | null;
  title?: string | null;
  url?: string | null;
  value?: string | null;
  zipCode?: string | null;
}

export interface Citation {
  address?: string | null;
  author?: string | null;
  availabilityStatement?: string | null;
  contentDesignator?: string | null;
  dateOfPublication?: string | null;
  dateOfRevision?: string | null;
  edition?: string | null;
  editor?: string | null;
  extent?: string | null;
  language?: string | null;
  location?: string | null;
  mediumDesignator?: string | null;
  notes?: string | null;
  organization?: string | null;
  placeOfPublication?: string | null;
  publisher?: string | null;
  series?: string | null;
  title?: string | null;
  unstructuredValue?: string | null;
}
