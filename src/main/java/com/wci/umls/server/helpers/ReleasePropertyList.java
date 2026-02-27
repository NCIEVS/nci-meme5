/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import com.wci.umls.server.model.algo.ReleaseProperty;

/**
 * Represents a sortable list of {@link ReleaseProperty}
 */
public interface ReleasePropertyList extends ResultList<ReleaseProperty> {
  // nothing extra, a simple wrapper for easy serialization
}
