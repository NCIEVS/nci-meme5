package com.wci.umls.server.helpers.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.junit.Test;

import com.wci.umls.server.jpa.model.content.AtomRelationshipJpa;
import com.wci.umls.server.jpa.model.content.CodeRelationshipJpa;
import com.wci.umls.server.jpa.model.content.ComponentInfoRelationshipJpa;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.jpa.model.content.ConceptRelationshipJpa;
import com.wci.umls.server.jpa.model.content.DescriptorRelationshipJpa;
import com.wci.umls.server.model.content.Component;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.content.Relationship;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/** Database-free regression tests for the shared rule and JPA callbacks. */
public class RelationshipPublishabilityUnitTest {

  @Test
  public void bequeathalsAreUnpublishableForProjectAndSourceOwners() {
    for (String owner : List.of("NCIMTH", "MTH")) {
      for (String type : List.of("BRO", "BRN", "BRB")) {
        ConceptRelationshipJpa relationship = relationship(type);
        relationship.setTerminology(owner);
        relationship.setVersion("NCIMTH".equals(owner) ? "latest" : "2026AA");
        relationship.setPublishable(true);

        RelationshipPublishability.enforce(relationship);

        assertFalse(owner + ":" + type, relationship.isPublishable());
      }
    }
  }

  @Test
  public void creationFlagOrderDoesNotAffectTheSaveRule() {
    for (String type : List.of("BRO", "BRN", "BRB")) {
      // Editor paths assign the type before the default publishable flag.
      ConceptRelationshipJpa editorRelationship = relationship(type);
      editorRelationship.setPublishable(true);

      // Loader paths read flags before assigning the normalized type.
      ConceptRelationshipJpa loadedRelationship = new ConceptRelationshipJpa();
      loadedRelationship.setPublishable(true);
      loadedRelationship.setRelationshipType(type);

      RelationshipPublishability.enforce(editorRelationship);
      RelationshipPublishability.enforce(loadedRelationship);

      assertFalse(editorRelationship.isPublishable());
      assertFalse(loadedRelationship.isPublishable());
    }
  }

  @Test
  public void persistCallbackProtectsBothDirections() throws Exception {
    for (Map.Entry<String, String> pair :
        Map.of("BRO", "BRO", "BRN", "BRB", "BRB", "BRN").entrySet()) {
      ConceptRelationshipJpa forward = relationship(pair.getKey());
      forward.setPublishable(true);
      forward.setAssertedDirection(true);
      ConceptRelationship reverse = (ConceptRelationship) forward.createInverseRelationship(
          forward, pair.getValue(), "");

      // Exercise the actual annotated hooks without starting a database service.
      invokeCallback(forward, PrePersist.class);
      invokeCallback(reverse, PrePersist.class);

      assertFalse(forward.isPublishable());
      assertFalse(reverse.isPublishable());
      assertEquals(pair.getValue(), reverse.getRelationshipType());
      assertSame(forward.getFrom(), reverse.getTo());
      assertSame(forward.getTo(), reverse.getFrom());
      assertTrue(forward.isAssertedDirection());
      assertFalse(reverse.isAssertedDirection());
    }
  }

  @Test
  public void updateCallbackPreventsRepublishingApprovedBequeathals() throws Exception {
    for (String type : List.of("BRO", "BRN", "BRB")) {
      ConceptRelationshipJpa relationship = relationship(type);
      relationship.setPublishable(false);
      relationship.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

      // A maintenance action can dirty a managed object without updateRelationship().
      relationship.setPublishable(true);
      invokeCallback(relationship, PreUpdate.class);

      assertFalse(relationship.isPublishable());
      assertEquals(WorkflowStatus.READY_FOR_PUBLICATION, relationship.getWorkflowStatus());
    }
  }

  @Test
  public void changingAnOrdinaryRelationshipToBequeathalClearsPublishability() throws Exception {
    ConceptRelationshipJpa relationship = relationship("RO");
    relationship.setPublishable(true);
    relationship.setRelationshipType("BRO");

    invokeCallback(relationship, PreUpdate.class);

    assertFalse(relationship.isPublishable());
  }

  @Test
  public void historicalPublishedFlagAndEndpointsArePreserved() {
    ConceptRelationshipJpa relationship = relationship("BRN");
    relationship.setPublished(true);
    relationship.setPublishable(true);
    relationship.setWorkflowStatus(WorkflowStatus.PUBLISHED);
    relationship.setTerminologyId("R123");

    RelationshipPublishability.enforce(relationship);
    RelationshipPublishability.enforce(relationship);

    assertFalse(relationship.isPublishable());
    assertTrue(relationship.isPublished());
    assertEquals(WorkflowStatus.PUBLISHED, relationship.getWorkflowStatus());
    assertEquals("R123", relationship.getTerminologyId());
    assertEquals("BRN", relationship.getRelationshipType());
    assertFalse(relationship.getFrom().isPublishable());
    assertTrue(relationship.getTo().isPublishable());
  }

  @Test
  public void ordinaryConceptRelationshipFlagsArePreserved() throws Exception {
    for (String type : new String[] {"RO", "RN", "RB", "XR", "BR_OTHER", null}) {
      for (boolean publishable : new boolean[] {false, true}) {
        ConceptRelationshipJpa relationship = relationship(type);
        relationship.setPublishable(publishable);

        RelationshipPublishability.enforce(relationship);
        invokeCallback(relationship, PrePersist.class);
        invokeCallback(relationship, PreUpdate.class);

        assertEquals(type, publishable, relationship.isPublishable());
      }
    }
  }

  @Test
  public void otherComponentKindsAreUnaffected() {
    for (Relationship<?, ?> relationship : List.of(new AtomRelationshipJpa(),
        new CodeRelationshipJpa(), new DescriptorRelationshipJpa(),
        new ComponentInfoRelationshipJpa())) {
      relationship.setRelationshipType("BRO");
      relationship.setPublishable(true);
      RelationshipPublishability.enforce(relationship);
      assertTrue(relationship.isPublishable());
    }
    Component concept = new ConceptJpa();
    concept.setPublishable(true);
    RelationshipPublishability.enforce(concept);
    assertTrue(concept.isPublishable());
  }

  @Test
  public void existingInvalidFlagsRemainVisibleUntilSave() throws Exception {
    ConceptRelationshipJpa stored = relationship("BRO");
    stored.setPublishable(true);
    ConceptRelationshipJpa copy = new ConceptRelationshipJpa(stored, false);

    assertTrue(stored.isPublishable());
    assertTrue(copy.isPublishable());

    invokeCallback(copy, PreUpdate.class);

    assertFalse(copy.isPublishable());
    assertTrue(stored.isPublishable());
  }

  private static ConceptRelationshipJpa relationship(String type) {
    ConceptRelationshipJpa relationship = new ConceptRelationshipJpa();
    relationship.setRelationshipType(type);
    ConceptJpa from = new ConceptJpa();
    from.setId(1L);
    from.setPublishable(false);
    ConceptJpa to = new ConceptJpa();
    to.setId(2L);
    to.setPublishable(true);
    relationship.setFrom(from);
    relationship.setTo(to);
    return relationship;
  }

  private static void invokeCallback(Object entity, Class<? extends Annotation> event)
    throws Exception {
    int invoked = 0;
    for (Method method : entity.getClass().getDeclaredMethods()) {
      if (method.isAnnotationPresent(event)) {
        method.invoke(entity);
        invoked++;
      }
    }
    assertEquals("Expected a callback for " + event.getSimpleName(), 1, invoked);
  }
}
