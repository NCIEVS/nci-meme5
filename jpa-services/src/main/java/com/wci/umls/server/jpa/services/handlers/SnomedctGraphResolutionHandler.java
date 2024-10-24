/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services.handlers;

import java.util.ArrayList;
import java.util.Properties;

import com.wci.umls.server.helpers.ComponentInfo;
import com.wci.umls.server.helpers.meta.GeneralMetadataEntryList;
import com.wci.umls.server.jpa.services.MetadataServiceJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.content.AtomSubsetMember;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.ComponentHasAttributes;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.content.ConceptSubsetMember;
import com.wci.umls.server.model.content.Definition;
import com.wci.umls.server.model.content.Relationship;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.meta.GeneralMetadataEntry;
import com.wci.umls.server.services.MetadataService;
import com.wci.umls.server.services.handlers.GraphResolutionHandler;

/**
 * Default implementation of {@link GraphResolutionHandler}. This connects
 * graphs at the level at which CascadeType.ALL is used in the data model.
 */
public class SnomedctGraphResolutionHandler
    extends DefaultGraphResolutionHandler {

  /** The atv prop. */
  private static Properties prop = null;

  /** The terminology. */
  private static String terminology = "SNOMEDCT";

  /** The version. */
  private static String version = "latest";

  /**
   * Cache properties.
   *
   * @throws Exception the exception
   */
  private synchronized static void cacheProperties() throws Exception {
    if (prop == null) {
      prop = new Properties();
      MetadataService service = new MetadataServiceJpa();
      GeneralMetadataEntryList list =
          service.getGeneralMetadataEntries(terminology, version);
      for (final GeneralMetadataEntry entry : list.getObjects()) {
        prop.setProperty(entry.getAbbreviation(), entry.getExpandedForm());
      }
    }
  }

  /* see superclass */
  @Override
  public void resolve(Concept concept) throws Exception {
    cacheProperties();

    if (concept != null) {

      boolean nullId = concept.getId() == null;
      concept.getLabels().size();

      // subset members
      for (final ConceptSubsetMember member : concept.getMembers()) {
        member.getTerminology();
        resolveAttributes(member, nullId);
      }

      // Attributes
      resolveAttributes(concept, nullId);

      // Component History
      resolveComponentHistory(concept, nullId);

      // Definitions
      for (final Definition def : concept.getDefinitions()) {
        resolveDefinition(def, nullId);
      }

      // Semantic type components
      for (final SemanticTypeComponent sty : concept.getSemanticTypes()) {
        if (nullId) {
          sty.setId(null);
        }
        sty.getSemanticType();
        resolve(sty);
      }

      // Atoms
      for (final Atom atom : concept.getAtoms()) {
        // if the concept is "new", then the atom must be too
        if (nullId) {
          atom.setId(null);
        }
        resolve(atom);
      }

      // Relationships
      // Default behavior -- do not return relationships, require paging calls
      concept.setRelationships(new ArrayList<ConceptRelationship>());

      // lazy initialization of user annotations
      concept.getNotes().size();

    } else if (concept == null) {
      throw new Exception("Cannot resolve a null concept.");
    }
  }

  /* see superclass */
  @Override
  public void resolve(Atom atom) throws Exception {
    cacheProperties();

    if (atom != null) {
      boolean nullId = atom.getId() == null;

      // subset members
      for (final AtomSubsetMember member : atom.getMembers()) {
        member.getTerminology();
        resolveAttributes(member, nullId);
      }

      atom.getName();
      atom.getConceptTerminologyIds().keySet();
      atom.getAlternateTerminologyIds().keySet();
      if (prop.getProperty(atom.getTermType()) != null) {
        atom.setTermType(prop.getProperty(atom.getTermType()));
      }

      // Attributes
      resolveAttributes(atom, nullId);

      // Component History
      resolveComponentHistory(atom, nullId);

      // Definitions
      for (final Definition def : atom.getDefinitions()) {
        resolveDefinition(def, nullId);
      }

      // skip rels
      atom.setRelationships(new ArrayList<AtomRelationship>());
      atom.getNotes().size();

    } else if (atom == null) {
      throw new Exception("Cannot resolve a null atom.");
    }

  }

  /* see superclass */
  @Override
  public void resolve(
    Relationship<? extends ComponentInfo, ? extends ComponentInfo> relationship)
    throws Exception {
    cacheProperties();
    if (relationship != null) {
      if (relationship.getFrom() != null) {
        relationship.getFrom().getTerminology();
      }
      if (relationship.getTo() != null) {
        relationship.getTo().getTerminology();
      }
      if (relationship.getAlternateTerminologyIds() != null) {
        relationship.getAlternateTerminologyIds().keySet();
      }

      if (prop
          .getProperty(relationship.getAdditionalRelationshipType()) != null) {
        relationship.setAdditionalRelationshipType(
            prop.getProperty(relationship.getAdditionalRelationshipType()));
      }
      resolveAttributes(relationship, relationship.getId() == null);
    } else if (relationship == null) {
      throw new Exception("Cannot resolve a null relationship.");
    }
  }

  /**
   * Resolve attributes.
   *
   * @param component the component
   * @param nullId the null id
   * @throws Exception the exception
   */
  @Override
  protected void resolveAttributes(ComponentHasAttributes component,
    boolean nullId) throws Exception {
    cacheProperties();
    for (final Attribute att : component.getAttributes()) {
      att.getName();
      att.getAlternateTerminologyIds().keySet();
      if (nullId) {
        att.setId(null);
      }
      if (prop.getProperty(att.getValue()) != null) {
        att.setValue(
            prop.getProperty(att.getValue()) + " (" + att.getValue() + ")");
      }
    }
  }

}
