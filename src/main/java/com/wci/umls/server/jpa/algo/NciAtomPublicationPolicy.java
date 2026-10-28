package com.wci.umls.server.jpa.algo;

import com.wci.umls.server.model.content.Atom;

/** Applies the NCI atom flags historically enforced by the release AdHoc. */
public final class NciAtomPublicationPolicy {

  private NciAtomPublicationPolicy() {
    // Utility class.
  }

  /**
   * Applies the NCIMTH override to an atom, preserving other publication flags.
   *
   * @param projectTerminology the owning project's terminology, not the atom's SAB
   * @param atom the atom
   * @return whether a flag changed and the atom needs to be persisted
   */
  public static boolean apply(String projectTerminology, Atom atom) {
    if (!"NCIMTH".equals(projectTerminology)) {
      return false;
    }

    final boolean obsoleteTermType =
        "OP".equals(atom.getTermType()) || "IS".equals(atom.getTermType());
    if (!"AB".equals(atom.getTermType()) && !obsoleteTermType) {
      return false;
    }

    boolean changed = false;
    if (!atom.isSuppressible()) {
      atom.setSuppressible(true);
      changed = true;
    }
    if (obsoleteTermType && !atom.isObsolete()) {
      atom.setObsolete(true);
      changed = true;
    }
    return changed;
  }
}
