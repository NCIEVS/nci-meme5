/**
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import com.wci.umls.server.model.admin.MaintenanceWindow;

/**
 * Represents a sortable list of {@link MaintenanceWindow}.
 */
public interface MaintenanceWindowList extends ResultList<MaintenanceWindow> {
  // nothing extra, a simple wrapper for easy serialization
}
