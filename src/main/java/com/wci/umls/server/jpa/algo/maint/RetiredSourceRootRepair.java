package com.wci.umls.server.jpa.algo.maint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.wci.umls.server.jpa.model.content.ConceptRelationshipJpa;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/** Validates and plans the three reviewed NM-298 root bequeathals without writes. */
final class RetiredSourceRootRepair {

  static final String ACTION_NAME = "Add missing retired source root bequeathals (NM-298)";

  record Target(long oldId, String oldCui, String vabCode, long rootId,
      String rootCui, String rabCode) {
  }

  static final List<Target> TARGETS = List.of(
      new Target(2859814L, "CL466586", "V-ICD10_2010",
          28704768L, "CL1793137", "V-ICD10"),
      new Target(43277739L, "C5978422", "V-RXNORM_2020AA_2025_03_03F",
          660568L, "C1140284", "V-RXNORM"),
      new Target(43277741L, "C5978423", "V-VANDF_2025_01_31",
          660569L, "C1140288", "V-VANDF"));

  private static final Set<String> BEQUEATHAL_TYPES = Set.of("BRO", "BRN", "BRB");

  private RetiredSourceRootRepair() {
    // Utility class.
  }

  static void checkProject(String terminology, String version) {
    require("NCIMTH".equals(terminology) && "latest".equals(version),
        "This repair requires project NCIMTH/latest");
  }

  /** Returns new forward relationships only after all three pairs pass review. */
  static List<ConceptRelationship> plan(Map<Long, Concept> concepts,
    Map<String, String> inverses) {
    final String broInverse = inverses.get("BRO");
    require(broInverse != null && BEQUEATHAL_TYPES.contains(broInverse)
        && "BRO".equals(inverses.get(broInverse)),
        "Missing or inconsistent BRO inverse metadata");

    final List<ConceptRelationship> additions = new ArrayList<>();
    for (final Target target : TARGETS) {
      final Concept old = concepts.get(target.oldId());
      final Concept root = concepts.get(target.rootId());
      checkConcept(old, target.oldId(), target.oldCui(), false);
      checkConcept(root, target.rootId(), target.rootCui(), true);
      require(hasAtom(old, "VAB", target.vabCode(), false),
          "Missing retired SRC/VAB " + target.vabCode() + " on " + target.oldId());
      require(hasAtom(root, "RAB", target.rabCode(), true),
          "Missing publishable SRC/RAB " + target.rabCode() + " on " + target.rootId());

      final List<ConceptRelationship> forward = between(old, root);
      final List<ConceptRelationship> reverse = between(root, old);
      if (!forward.isEmpty() || !reverse.isEmpty()) {
        require(forward.size() == 1 && reverse.size() == 1
            && validExistingPair(forward.get(0), reverse.get(0), inverses),
            "Conflicting, incomplete, duplicate, or publishable root pair: "
                + target.oldId() + " -> " + target.rootId()
                + "; review existing relationships before rerunning");
        continue;
      }

      final ConceptRelationshipJpa relationship = new ConceptRelationshipJpa();
      relationship.setFrom(old);
      relationship.setTo(root);
      relationship.setTerminology("NCIMTH");
      relationship.setVersion("latest");
      relationship.setTerminologyId("");
      relationship.setRelationshipType("BRO");
      relationship.setAdditionalRelationshipType("");
      relationship.setGroup("");
      relationship.setAssertedDirection(true);
      relationship.setPublishable(false);
      relationship.setPublished(false);
      relationship.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
      additions.add(relationship);
    }
    return additions;
  }

  private static void checkConcept(Concept concept, long id, String cui,
    boolean publishable) {
    require(concept != null && Long.valueOf(id).equals(concept.getId())
        && cui.equals(concept.getTerminologyId())
        && "NCIMTH".equals(concept.getTerminology())
        && "latest".equals(concept.getVersion())
        && concept.isPublishable() == publishable,
        "Concept identity or publishability changed: " + id + " / " + cui);
  }

  private static boolean hasAtom(Concept concept, String termType, String code,
    boolean publishable) {
    return concept.getAtoms().stream().anyMatch(atom ->
        "SRC".equals(atom.getTerminology()) && termType.equals(atom.getTermType())
            && code.equals(atom.getCodeId()) && atom.isPublishable() == publishable);
  }

  private static List<ConceptRelationship> between(Concept from, Concept to) {
    return from.getRelationships().stream().filter(relationship ->
        relationship.getTo() != null
            && to.getId().equals(relationship.getTo().getId())).toList();
  }

  private static boolean validExistingPair(ConceptRelationship forward,
    ConceptRelationship reverse, Map<String, String> inverses) {
    return BEQUEATHAL_TYPES.contains(forward.getRelationshipType())
        && BEQUEATHAL_TYPES.contains(reverse.getRelationshipType())
        && reverse.getRelationshipType().equals(inverses.get(forward.getRelationshipType()))
        && forward.getRelationshipType().equals(inverses.get(reverse.getRelationshipType()))
        && Objects.equals(forward.getTerminology(), reverse.getTerminology())
        && Objects.equals(forward.getVersion(), reverse.getVersion())
        && Objects.equals(forward.getAdditionalRelationshipType(),
            reverse.getAdditionalRelationshipType())
        && Objects.equals(forward.getGroup(), reverse.getGroup())
        && !forward.isPublishable() && !reverse.isPublishable();
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }
}
