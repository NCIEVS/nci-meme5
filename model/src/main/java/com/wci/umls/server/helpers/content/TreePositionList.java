/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers.content;

import com.wci.umls.server.helpers.ResultList;
import com.wci.umls.server.model.content.ComponentHasAttributesAndName;
import com.wci.umls.server.model.content.TreePosition;

/**
 * Represents a sortable list of {@link TreePosition}
 */
public interface TreePositionList extends
    ResultList<TreePosition<? extends ComponentHasAttributesAndName>> {
  // nothing extra, a simple wrapper for easy serialization
}
