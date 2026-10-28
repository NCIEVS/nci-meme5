package com.wci.umls.server.jpa.algo.maint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.wci.umls.server.jpa.model.content.AtomJpa;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.jpa.model.content.ConceptRelationshipJpa;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/** Database-free checks of the scoped repair's preconditions and rerun behavior. */
public class RetiredSourceRootRepairUnitTest {

  private Map<Long, Concept> concepts;

  private final Map<String, String> inverses =
      Map.of("BRO", "BRO", "BRN", "BRB", "BRB", "BRN");

  @Before
  public void setUp() {
    concepts = new HashMap<>();
    addConcept(2859814L, "CL466586", false, "VAB", "V-ICD10_2010");
    addConcept(28704768L, "CL1793137", true, "RAB", "V-ICD10");
    addConcept(43277739L, "C5978422", false, "VAB", "V-RXNORM_2020AA_2025_03_03F");
    addConcept(660568L, "C1140284", true, "RAB", "V-RXNORM");
    addConcept(43277741L, "C5978423", false, "VAB", "V-VANDF_2025_01_31");
    addConcept(660569L, "C1140288", true, "RAB", "V-VANDF");
  }

  @Test
  public void plansExactlyTheReviewedPairsWithUnpublishableFlags() {
    final List<ConceptRelationship> plan = RetiredSourceRootRepair.plan(concepts, inverses);
    assertEquals(Set.of("2859814->28704768", "43277739->660568", "43277741->660569"),
        plan.stream().map(r -> r.getFrom().getId() + "->" + r.getTo().getId())
            .collect(Collectors.toSet()));
    assertEquals(3, plan.size());
    for (final ConceptRelationship relationship : plan) {
      assertNull(relationship.getId());
      assertEquals("BRO", relationship.getRelationshipType());
      assertEquals("", relationship.getAdditionalRelationshipType());
      assertEquals("NCIMTH", relationship.getTerminology());
      assertEquals("latest", relationship.getVersion());
      assertEquals(WorkflowStatus.READY_FOR_PUBLICATION, relationship.getWorkflowStatus());
      assertFalse(relationship.isPublishable());
      assertFalse(relationship.isPublished());
      assertFalse(relationship.getFrom().isPublishable());
      assertTrue(relationship.getTo().isPublishable());
    }
    assertTrue(concepts.values().stream().allMatch(c -> c.getRelationships().isEmpty()));
  }

  @Test
  public void rerunSkipsCompletedPairs() {
    for (final ConceptRelationship forward : RetiredSourceRootRepair.plan(concepts, inverses)) {
      attachPair(forward);
    }
    assertTrue(RetiredSourceRootRepair.plan(concepts, inverses).isEmpty());
    assertEquals(6, concepts.values().stream().mapToInt(c -> c.getRelationships().size()).sum());
  }

  @Test
  public void preservesOtherDestinationsAndSourceOwnedBrnPairs() {
    final Concept old = concepts.get(2859814L);
    final Concept destination = new ConceptJpa();
    destination.setId(658023L);
    final ConceptRelationship historical = relationship(old, destination, "BRO");
    old.getRelationships().add(historical);

    final ConceptRelationship existing =
        relationship(concepts.get(43277739L), concepts.get(660568L), "BRN");
    existing.setTerminology("MTH");
    existing.setVersion("2026AA");
    existing.setPublished(true);
    attachPair(existing);

    final List<ConceptRelationship> plan = RetiredSourceRootRepair.plan(concepts, inverses);
    assertEquals(2, plan.size());
    assertTrue(plan.stream().noneMatch(r -> r.getFrom().getId() == 43277739L));
    assertSame(historical, old.getRelationships().get(0));
    assertEquals("BRN", existing.getRelationshipType());
    assertTrue(existing.isPublished());
    assertEquals("MTH", existing.getTerminology());
  }

  @Test
  public void rejectsChangedEndpointsOrAtomsBeforeReturningAnyPlan() {
    final List<Consumer<Map<Long, Concept>>> changes = List.of(
        map -> map.remove(660569L),
        map -> map.get(660569L).setId(1L),
        map -> map.get(660569L).setTerminologyId("WRONG"),
        map -> map.get(660569L).setTerminology("OTHER"),
        map -> map.get(660569L).setVersion("2025AA"),
        map -> map.get(660569L).setPublishable(false),
        map -> map.get(43277741L).setPublishable(true),
        map -> map.get(660569L).getAtoms().get(0).setPublishable(false),
        map -> map.get(660569L).getAtoms().get(0).setCodeId("V-RXNORM"),
        map -> map.get(660569L).getAtoms().get(0).setTermType("VAB"),
        map -> map.get(43277741L).getAtoms().get(0).setPublishable(true),
        map -> map.get(43277741L).getAtoms().get(0).setTerminology("OTHER"),
        map -> map.get(43277741L).getAtoms().get(0).setCodeId("V-VANDF_2026"));
    for (final Consumer<Map<Long, Concept>> change : changes) {
      setUp();
      change.accept(concepts);
      assertThrows(IllegalStateException.class,
          () -> RetiredSourceRootRepair.plan(concepts, inverses));
      assertTrue(concepts.values().stream().allMatch(c -> c.getRelationships().isEmpty()));
    }
  }

  @Test
  public void rejectsIncompletePairInEitherDirection() {
    final Concept old = concepts.get(43277741L);
    final Concept root = concepts.get(660569L);
    old.getRelationships().add(relationship(old, root, "BRO"));
    assertThrows(IllegalStateException.class,
        () -> RetiredSourceRootRepair.plan(concepts, inverses));
    old.getRelationships().clear();
    root.getRelationships().add(relationship(root, old, "BRO"));
    assertThrows(IllegalStateException.class,
        () -> RetiredSourceRootRepair.plan(concepts, inverses));
  }

  @Test
  public void rejectsPublishableOrMismatchedExistingPairs() {
    final List<Consumer<ConceptRelationship>> changes = List.of(
        r -> r.setPublishable(true),
        r -> r.setRelationshipType("RO"),
        r -> r.setRelationshipType("BRN"),
        r -> r.setTerminology("MTH"),
        r -> r.setVersion("2026AA"),
        r -> r.setAdditionalRelationshipType("different"),
        r -> r.setGroup("different"));
    for (final Consumer<ConceptRelationship> change : changes) {
      setUp();
      final ConceptRelationship forward =
          relationship(concepts.get(43277741L), concepts.get(660569L), "BRO");
      attachPair(forward);
      change.accept(forward);
      assertThrows(IllegalStateException.class,
          () -> RetiredSourceRootRepair.plan(concepts, inverses));
      assertSame(forward, concepts.get(43277741L).getRelationships().get(0));
    }
  }

  @Test
  public void rejectsDuplicatePairs() {
    attachPair(relationship(concepts.get(43277741L), concepts.get(660569L), "BRO"));
    attachPair(relationship(concepts.get(43277741L), concepts.get(660569L), "BRO"));
    assertThrows(IllegalStateException.class,
        () -> RetiredSourceRootRepair.plan(concepts, inverses));
  }

  @Test
  public void rejectsMissingOrInconsistentInverseMetadata() {
    assertThrows(IllegalStateException.class,
        () -> RetiredSourceRootRepair.plan(concepts, Map.of()));
    assertThrows(IllegalStateException.class,
        () -> RetiredSourceRootRepair.plan(concepts, Map.of("BRO", "RO", "RO", "BRO")));
    assertThrows(IllegalStateException.class,
        () -> RetiredSourceRootRepair.plan(concepts, Map.of("BRO", "BRN", "BRN", "BRB")));
  }

  @Test
  public void requiresTheReviewedProject() {
    RetiredSourceRootRepair.checkProject("NCIMTH", "latest");
    assertThrows(IllegalStateException.class,
        () -> RetiredSourceRootRepair.checkProject("MTH", "latest"));
    assertThrows(IllegalStateException.class,
        () -> RetiredSourceRootRepair.checkProject("NCIMTH", "2026AA"));
  }

  private void addConcept(long id, String cui, boolean publishable, String type, String code) {
    final Concept concept = new ConceptJpa();
    concept.setId(id);
    concept.setTerminologyId(cui);
    concept.setTerminology("NCIMTH");
    concept.setVersion("latest");
    concept.setPublishable(publishable);
    final AtomJpa atom = new AtomJpa();
    atom.setTerminology("SRC");
    atom.setTermType(type);
    atom.setCodeId(code);
    atom.setPublishable(publishable);
    concept.getAtoms().add(atom);
    concepts.put(id, concept);
  }

  private ConceptRelationship relationship(Concept from, Concept to, String type) {
    final ConceptRelationshipJpa relationship = new ConceptRelationshipJpa();
    relationship.setFrom(from);
    relationship.setTo(to);
    relationship.setRelationshipType(type);
    relationship.setAdditionalRelationshipType("");
    relationship.setGroup("");
    relationship.setTerminology("NCIMTH");
    relationship.setVersion("latest");
    relationship.setPublishable(false);
    return relationship;
  }

  private void attachPair(ConceptRelationship forward) {
    final ConceptRelationshipJpa reverse = new ConceptRelationshipJpa(forward, false);
    reverse.setFrom(forward.getTo());
    reverse.setTo(forward.getFrom());
    reverse.setRelationshipType(inverses.get(forward.getRelationshipType()));
    forward.getFrom().getRelationships().add(forward);
    reverse.getFrom().getRelationships().add(reverse);
  }
}
