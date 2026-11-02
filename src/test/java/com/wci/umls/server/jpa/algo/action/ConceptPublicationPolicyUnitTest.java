package com.wci.umls.server.jpa.algo.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.wci.umls.server.jpa.model.content.AtomJpa;
import com.wci.umls.server.jpa.model.content.AttributeJpa;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.jpa.model.content.ConceptRelationshipJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/** Database-free regression checks for restoring publication of surviving CUIs. */
public class ConceptPublicationPolicyUnitTest {

  @Test
  public void restoresRetiredCuiAfterPublishableSourceAtomsAreAdded() {
    final Concept concept = retiredConcept();
    final Atom retiredAtom = atom("SNOMEDCT_US", false);
    retiredAtom.setVersion("2026_03_01");
    concept.getAtoms().add(retiredAtom);
    assertFalse(ConceptPublicationPolicy.restorePublishability(concept));

    final Atom currentAtom = atom("SNOMEDCT_US", true);
    currentAtom.setVersion("2026_09_01");
    concept.getAtoms().add(currentAtom);

    assertTrue(ConceptPublicationPolicy.restorePublishability(concept));
    assertTrue(concept.isPublishable());
    assertFalse(retiredAtom.isPublishable());
    assertTrue(currentAtom.isPublishable());
    assertEquals("C0361226", concept.getTerminologyId());
    assertEquals(WorkflowStatus.READY_FOR_PUBLICATION, concept.getWorkflowStatus());
    assertTrue(concept.isPublished());
    assertFalse(ConceptPublicationPolicy.restorePublishability(concept));
  }

  @Test
  public void leavesEmptyRetiredConceptUnpublishable() {
    final Concept concept = retiredConcept();

    assertFalse(ConceptPublicationPolicy.restorePublishability(concept));
    assertFalse(concept.isPublishable());
  }

  @Test
  public void leavesConceptWithOnlyUnpublishableAtomsUnpublishable() {
    final Concept concept = retiredConcept();
    concept.getAtoms().add(atom("SNOMEDCT_US", false));
    concept.getAtoms().add(atom("MTH", false));

    assertFalse(ConceptPublicationPolicy.restorePublishability(concept));
    assertFalse(concept.isPublishable());
  }

  @Test
  public void preservesExistingPublicationWhenRetirementMustBeHandledByTheAction() {
    final Concept concept = retiredConcept();
    concept.setPublishable(true);
    concept.getAtoms().add(atom("SNOMEDCT_US", false));

    assertFalse(ConceptPublicationPolicy.restorePublishability(concept));
    assertTrue(concept.isPublishable());
  }

  @Test
  public void alreadyPublishableConceptDoesNotRequireAnotherPublicationChange() {
    final Concept concept = retiredConcept();
    concept.setPublishable(true);
    concept.getAtoms().add(atom("MSH", true));

    assertFalse(ConceptPublicationPolicy.restorePublishability(concept));
    assertTrue(concept.isPublishable());
  }

  @Test
  public void retainsMatrixInitializerCriterionForMthPreferredNames() {
    final Concept concept = retiredConcept();
    final Atom preferredName = atom("MTH", true);
    preferredName.setTermType("PN");
    concept.getAtoms().add(preferredName);

    assertTrue(ConceptPublicationPolicy.restorePublishability(concept));
    assertTrue(concept.isPublishable());
    assertTrue(preferredName.isPublishable());
  }

  @Test
  public void doesNotReactivateHistoricalRelationshipsOrAttributes() {
    final Concept concept = retiredConcept();
    concept.getAtoms().add(atom("SNOMEDCT_US", true));
    final ConceptRelationship outgoing = retiredRelationship(10L);
    final ConceptRelationship incoming = retiredRelationship(11L);
    concept.getRelationships().add(outgoing);
    concept.getInverseRelationships().add(incoming);
    final Attribute conceptAttribute = new AttributeJpa();
    conceptAttribute.setPublishable(false);
    concept.getAttributes().add(conceptAttribute);

    assertTrue(ConceptPublicationPolicy.restorePublishability(concept));
    assertTrue(concept.isPublishable());
    assertFalse(outgoing.isPublishable());
    assertFalse(incoming.isPublishable());
    assertFalse(outgoing.getAttributes().get(0).isPublishable());
    assertFalse(incoming.getAttributes().get(0).isPublishable());
    assertFalse(conceptAttribute.isPublishable());
  }

  private static Concept retiredConcept() {
    final Concept concept = new ConceptJpa();
    concept.setId(190693L);
    concept.setTerminology("NCIMTH");
    concept.setVersion("latest");
    concept.setTerminologyId("C0361226");
    concept.setName("Garlic- and kelp-containing product");
    concept.setPublishable(false);
    concept.setPublished(true);
    concept.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    return concept;
  }

  private static Atom atom(String terminology, boolean publishable) {
    final Atom atom = new AtomJpa();
    atom.setTerminology(terminology);
    atom.setPublishable(publishable);
    return atom;
  }

  private static ConceptRelationship retiredRelationship(Long id) {
    final ConceptRelationship relationship = new ConceptRelationshipJpa();
    relationship.setId(id);
    relationship.setPublishable(false);
    final Attribute attribute = new AttributeJpa();
    attribute.setPublishable(false);
    relationship.getAttributes().add(attribute);
    return relationship;
  }
}
