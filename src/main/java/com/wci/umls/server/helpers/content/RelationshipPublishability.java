package com.wci.umls.server.helpers.content;

import com.wci.umls.server.model.content.Component;
import com.wci.umls.server.model.content.ConceptRelationship;

/** Publication rules shared by concept relationship writers. */
public final class RelationshipPublishability {

  private RelationshipPublishability() {
    // Utility class.
  }

  /**
   * Keeps concept bequeathals out of releases, regardless of their owner or
   * workflow status. Does not change the historical published flag.
   *
   * @param component the component being saved
   */
  public static void enforce(Component component) {
    if (component instanceof ConceptRelationship relationship) {
      final String type = relationship.getRelationshipType();
      if ("BRO".equals(type) || "BRN".equals(type) || "BRB".equals(type)) {
        relationship.setPublishable(false);
      }
    }
  }
}
