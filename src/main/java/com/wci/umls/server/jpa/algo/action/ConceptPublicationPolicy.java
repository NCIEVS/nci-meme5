package com.wci.umls.server.jpa.algo.action;

import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Concept;

/** Restores concept publication after molecular actions repopulate a concept. */
final class ConceptPublicationPolicy {

  private ConceptPublicationPolicy() {
    // Utility class.
  }

  /**
   * Restores publication using the same atom criterion as Matrix Initializer.
   * Retirement and its relationship cascades remain the responsibility of the
   * calling action. Historical component publication flags are left intact.
   *
   * @param concept the surviving or approved concept
   * @return true if the concept was made publishable
   */
  static boolean restorePublishability(Concept concept) {
    if (concept.isPublishable()) {
      return false;
    }
    for (final Atom atom : concept.getAtoms()) {
      if (atom.isPublishable()) {
        concept.setPublishable(true);
        return true;
      }
    }
    return false;
  }
}
