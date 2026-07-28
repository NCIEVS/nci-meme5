export interface MaintenanceWindow {
  endDate?: string | number | null;
  id?: number | null;
  lastModified?: string | number | null;
  lastModifiedBy?: string | null;
  startDate?: string | number | null;
  timestamp?: string | number | null;
}

export interface MaintenanceWindowListResponse {
  maintenanceWindows?: MaintenanceWindow[];
  objects?: MaintenanceWindow[];
  totalCount?: number;
}
