export type WorklistMode = 'Available' | 'Assigned' | 'Done' | 'Checklists';

export interface WorkflowWorklist {
  authors?: string[] | null;
  id?: number | null;
  lastModified?: string | number | null;
  name?: string | null;
  reviewers?: string[] | null;
  workflowStateHistory?: Record<string, number> | null;
  workflowStatus?: string | null;
}

export interface WorkflowConcept {
  id?: number | null;
  name?: string | null;
  workflowStatus?: string | null;
}

export interface WorkflowTrackingRecord {
  clusterId?: number | null;
  concepts?: WorkflowConcept[] | null;
  id?: number | null;
  lastModified?: string | number | null;
  workflowStatus?: string | null;
}

export interface WorkflowWorklistResponse {
  objects?: WorkflowWorklist[] | null;
  totalCount?: number | null;
  worklists?: WorkflowWorklist[] | null;
}

export interface WorkflowChecklistResponse {
  checklists?: WorkflowWorklist[] | null;
  objects?: WorkflowWorklist[] | null;
  totalCount?: number | null;
}

export interface WorkflowRecordResponse {
  objects?: WorkflowTrackingRecord[] | null;
  records?: WorkflowTrackingRecord[] | null;
  totalCount?: number | null;
}
