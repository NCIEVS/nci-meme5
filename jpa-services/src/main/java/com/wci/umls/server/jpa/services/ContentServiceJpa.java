/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.metamodel.EntityType;

import org.apache.log4j.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PfsParameter;
import com.wci.umls.server.helpers.SearchCriteriaList;
import com.wci.umls.server.helpers.SearchResultList;
import com.wci.umls.server.helpers.content.CodeList;
import com.wci.umls.server.helpers.content.ConceptList;
import com.wci.umls.server.helpers.content.DescriptorList;
import com.wci.umls.server.helpers.content.LexicalClassList;
import com.wci.umls.server.helpers.content.StringClassList;
import com.wci.umls.server.helpers.content.SubsetList;
import com.wci.umls.server.helpers.content.SubsetMemberList;
import com.wci.umls.server.jpa.content.AbstractComponentHasAttributes;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.jpa.content.CodeJpa;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.jpa.content.DefinitionJpa;
import com.wci.umls.server.jpa.content.DescriptorJpa;
import com.wci.umls.server.jpa.content.LexicalClassJpa;
import com.wci.umls.server.jpa.content.SemanticTypeComponentJpa;
import com.wci.umls.server.jpa.content.StringClassJpa;
import com.wci.umls.server.jpa.helpers.IndexUtility;
import com.wci.umls.server.jpa.helpers.content.CodeListJpa;
import com.wci.umls.server.jpa.helpers.content.ConceptListJpa;
import com.wci.umls.server.jpa.helpers.content.DescriptorListJpa;
import com.wci.umls.server.jpa.helpers.content.LexicalClassListJpa;
import com.wci.umls.server.jpa.helpers.content.StringClassListJpa;
import com.wci.umls.server.jpa.meta.AbstractAbbreviation;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomClass;
import com.wci.umls.server.model.content.Code;
import com.wci.umls.server.model.content.Component;
import com.wci.umls.server.model.content.ComponentHasAttributes;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.Definition;
import com.wci.umls.server.model.content.Descriptor;
import com.wci.umls.server.model.content.LexicalClass;
import com.wci.umls.server.model.content.Relationship;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.content.StringClass;
import com.wci.umls.server.model.content.Subset;
import com.wci.umls.server.model.content.TransitiveRelationship;
import com.wci.umls.server.services.ContentService;
import com.wci.umls.server.services.handlers.ComputePreferredNameHandler;
import com.wci.umls.server.services.handlers.GraphResolutionHandler;
import com.wci.umls.server.services.handlers.IdentifierAssignmentHandler;
import com.wci.umls.server.services.handlers.WorkflowListener;

/**
 * JPA enabled implementation of {@link ContentService}.
 */
public class ContentServiceJpa extends MetadataServiceJpa implements
    ContentService {

  /** The assign identifiers flag. */
  protected boolean assignIdentifiersFlag = false;

  /** The id assignment handler . */
  public static Map<String, IdentifierAssignmentHandler> idHandlerMap =
      new HashMap<>();
  static {

    try {
      if (config == null)
        config = ConfigUtility.getConfigProperties();
      String key = "identifier.assignment.handler";
      for (String handlerName : config.getProperty(key).split(",")) {
        if (handlerName.isEmpty())
          continue;
        // Add handlers to map
        IdentifierAssignmentHandler handlerService =
            ConfigUtility.newStandardHandlerInstanceWithConfiguration(key,
                handlerName, IdentifierAssignmentHandler.class);
        idHandlerMap.put(handlerName, handlerService);
      }
    } catch (Exception e) {
      e.printStackTrace();
      idHandlerMap = null;
    }
  }

  /** The helper map. */
  private static Map<String, ComputePreferredNameHandler> pnHandlerMap = null;
  static {
    pnHandlerMap = new HashMap<>();

    try {
      config = ConfigUtility.getConfigProperties();
      String key = "compute.preferred.name.handler";
      for (String handlerName : config.getProperty(key).split(",")) {

        // Add handlers to map
        ComputePreferredNameHandler handlerService =
            ConfigUtility.newStandardHandlerInstanceWithConfiguration(key,
                handlerName, ComputePreferredNameHandler.class);
        pnHandlerMap.put(handlerName, handlerService);
      }
    } catch (Exception e) {
      e.printStackTrace();
      pnHandlerMap = null;
    }
  }

  /** The graph resolver. */
  public static Map<String, GraphResolutionHandler> graphResolverMap = null;
  static {
    graphResolverMap = new HashMap<>();
    try {
      if (config == null)
        config = ConfigUtility.getConfigProperties();
      String key = "graph.resolution.handler";
      for (String handlerName : config.getProperty(key).split(",")) {
        if (handlerName.isEmpty())
          continue;
        // Add handlers to map
        GraphResolutionHandler handlerService =
            ConfigUtility.newStandardHandlerInstanceWithConfiguration(key,
                handlerName, GraphResolutionHandler.class);
        graphResolverMap.put(handlerName, handlerService);
      }

    } catch (Exception e) {
      e.printStackTrace();
      graphResolverMap = null;
    }
  }

  /** The concept field names. */
  private static String[] conceptFieldNames = {};
  static {

    try {
      conceptFieldNames =
          IndexUtility.getIndexedStringFieldNames(ConceptJpa.class).toArray(
              new String[] {});

    } catch (Exception e) {
      e.printStackTrace();
      conceptFieldNames = null;
    }
  }

  /**
   * Instantiates an empty {@link ContentServiceJpa}.
   *
   * @throws Exception the exception
   */
  public ContentServiceJpa() throws Exception {
    super();

    if (listeners == null) {
      throw new Exception(
          "Listeners did not properly initialize, serious error.");
    }
    if (graphResolverMap == null) {
      throw new Exception(
          "Graph resolver did not properly initialize, serious error.");
    }

    if (idHandlerMap == null) {
      throw new Exception(
          "Identifier assignment handler did not properly initialize, serious error.");
    }

    if (pnHandlerMap == null) {
      throw new Exception(
          "Preferred name handler did not properly initialize, serious error.");
    }

    if (conceptFieldNames == null) {
      throw new Exception(
          "Concept indexed field names did not properly initialize, serious error.");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.services.ContentService#getConcept(java.lang.Long)
   */
  @Override
  public Concept getConcept(Long id) throws Exception {
    Logger.getLogger(getClass()).debug("Content Service - get concept " + id);
    Concept c = manager.find(ConceptJpa.class, id);
    return c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getConcepts(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public ConceptList getConcepts(String terminologyId, String terminology,
    String version) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get concepts " + terminologyId + "/" + terminology
            + "/" + version);
    javax.persistence.Query query =
        manager.createQuery("select c from ConceptJpa c where "
            + "terminologyId = :terminologyId and " + ""
            + "terminologyVersion = :version and terminology = :terminology");

    // Try to retrieve the single expected result If zero or more than one
    // result are returned, log error and set result to null
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      @SuppressWarnings("unchecked")
      List<Concept> m = query.getResultList();
      ConceptListJpa conceptList = new ConceptListJpa();
      conceptList.setObjects(m);
      conceptList.setTotalCount(m.size());
      return conceptList;

    } catch (NoResultException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getConcept(java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Concept getConcept(String terminologyId, String terminology,
    String version, String branch) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get concept " + terminologyId + "/" + terminology
            + "/" + version + "/" + branch);
    assert branch != null;
    ConceptList list = getConcepts(terminologyId, terminology, version);
    if (list == null || list.getTotalCount() == 0) {
      Logger.getLogger(getClass()).debug("  no concept ");
      return null;
    }
    // Find the one matching the branch (or without having been branched to)
    for (Concept obj : list.getObjects()) {
      if (obj.getBranch().equals(branch)) {
        return obj;
      }
    }
    // TODO: deal with branching

    // If nothing found, return null;
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#addConcept(com.wci.umls.server
   * .model.content.Concept)
   */
  @Override
  public Concept addConcept(Concept concept) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - add concept " + concept.getTerminologyId());
    // Assign id
    IdentifierAssignmentHandler idHandler = null;
    if (assignIdentifiersFlag) {
      idHandler = idHandlerMap.get(concept.getTerminology());
      if (idHandler == null) {
        throw new Exception("Unable to find id handler for "
            + concept.getTerminology());
      }
      String id = idHandler.getTerminologyId(concept);
      concept.setTerminologyId(id);
    }

    // Add component
    Concept newConcept = addComponent(concept);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.conceptChanged(newConcept, WorkflowListener.Action.ADD);
      }
    }
    return newConcept;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#updateConcept(com.wci.umls.
   * server.model.content.Concept)
   */
  @Override
  public void updateConcept(Concept concept) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - update concept " + concept.getTerminologyId());

    // Id assignment should not change
    final IdentifierAssignmentHandler idHandler =
        idHandlerMap.get(concept.getTerminology());
    if (assignIdentifiersFlag) {
      if (!idHandler.allowConceptIdChangeOnUpdate()) {
        Concept concept2 = getConcept(concept.getId());
        if (!idHandler.getTerminologyId(concept).equals(
            idHandler.getTerminologyId(concept2))) {
          throw new Exception(
              "Update cannot be used to change object identity.");
        }
      } else {
        // set concept id on update
        concept.setTerminologyId(idHandler.getTerminologyId(concept));
      }
    }
    // update component
    this.updateComponent(concept);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.conceptChanged(concept, WorkflowListener.Action.UPDATE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#removeConcept(java.lang.Long)
   */
  @Override
  public void removeConcept(Long id) throws Exception {
    Logger.getLogger(getClass())
        .debug("Content Service - remove concept " + id);
    // Remove the component
    Concept concept = removeComponent(id, ConceptJpa.class);

    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.conceptChanged(concept, WorkflowListener.Action.REMOVE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.services.ContentService#getSubset(java.lang.Long)
   */
  @Override
  public Subset getSubset(Long id) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getSubsets(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public SubsetList getSubsets(String terminologyId, String terminology,
    String version) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getSubset(java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Subset getSubset(String terminologyId, String terminology,
    String version, String branch) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getSubsetMembers(java.lang.
   * String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public SubsetMemberList getSubsetMembers(String subsetId, String terminology,
    String version, String branch) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getAtomSubsetMembers(java.lang
   * .String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public SubsetMemberList getAtomSubsetMembers(String atomId,
    String terminology, String version, String branch) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getConceptSubsetMembers(java
   * .lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public SubsetMemberList getConceptSubsetMembers(String conceptId,
    String terminology, String version, String branch) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getAllSubsets(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public ConceptList getAllSubsets(String terminology, String version,
    String branch) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#addDefinition(com.wci.umls.
   * server .model.content.Definition)
   */
  @Override
  public Definition addDefinition(Definition definition) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - add definition " + definition.getTerminologyId());
    // Assign id
    IdentifierAssignmentHandler idHandler = null;
    if (assignIdentifiersFlag) {
      idHandler = idHandlerMap.get(definition.getTerminology());
      if (idHandler == null) {
        throw new Exception("Unable to find id handler for "
            + definition.getTerminology());
      }
      String id = idHandler.getTerminologyId(definition);
      definition.setTerminologyId(id);
    }

    // Add component
    Definition newDefinition = addComponent(definition);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.definitionChanged(newDefinition, WorkflowListener.Action.ADD);
      }
    }
    return newDefinition;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#updateDefinition(com.wci.umls.
   * server.model.content.Definition)
   */
  @Override
  public void updateDefinition(Definition definition) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - update definition " + definition.getTerminologyId());

    // Id assignment should not change
    final IdentifierAssignmentHandler idHandler =
        idHandlerMap.get(definition.getTerminology());
    if (assignIdentifiersFlag) {
      if (!idHandler.allowIdChangeOnUpdate()) {
        Definition definition2 =
            getComponent(definition.getId(), DefinitionJpa.class);
        if (!idHandler.getTerminologyId(definition).equals(
            idHandler.getTerminologyId(definition2))) {
          throw new Exception(
              "Update cannot be used to change object identity.");
        }
      } else {
        // set definition id on update
        definition.setTerminologyId(idHandler.getTerminologyId(definition));
      }
    }
    // update component
    this.updateComponent(definition);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.definitionChanged(definition, WorkflowListener.Action.UPDATE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#removeDefinition(java.lang.
   * Long)
   */
  @Override
  public void removeDefinition(Long id) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - remove definition " + id);
    // Remove the component
    Definition definition = removeComponent(id, DefinitionJpa.class);

    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.definitionChanged(definition, WorkflowListener.Action.REMOVE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#addSemanticTypeComponent(com
   * .wci.umls.server .model.content.SemanticTypeComponent)
   */
  @Override
  public SemanticTypeComponent addSemanticTypeComponent(
    SemanticTypeComponent semanticTypeComponent) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - add semanticTypeComponent "
            + semanticTypeComponent.getTerminologyId());
    // Assign id
    IdentifierAssignmentHandler idHandler = null;
    if (assignIdentifiersFlag) {
      idHandler = idHandlerMap.get(semanticTypeComponent.getTerminology());
      if (idHandler == null) {
        throw new Exception("Unable to find id handler for "
            + semanticTypeComponent.getTerminology());
      }
      String id = idHandler.getTerminologyId(semanticTypeComponent);
      semanticTypeComponent.setTerminologyId(id);
    }

    // Add component
    SemanticTypeComponent newSemanticTypeComponent =
        addComponent(semanticTypeComponent);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.semanticTypeChanged(newSemanticTypeComponent,
            WorkflowListener.Action.ADD);
      }
    }
    return newSemanticTypeComponent;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#updateSemanticTypeComponent
   * (com.wci.umls. server.model.content.SemanticTypeComponent)
   */
  @Override
  public void updateSemanticTypeComponent(
    SemanticTypeComponent semanticTypeComponent) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - update semanticTypeComponent "
            + semanticTypeComponent.getTerminologyId());

    // Id assignment should not change
    final IdentifierAssignmentHandler idHandler =
        idHandlerMap.get(semanticTypeComponent.getTerminology());
    if (assignIdentifiersFlag) {
      if (!idHandler.allowIdChangeOnUpdate()) {
        SemanticTypeComponent semanticTypeComponent2 =
            getComponent(semanticTypeComponent.getId(),
                SemanticTypeComponent.class);
        if (!idHandler.getTerminologyId(semanticTypeComponent).equals(
            idHandler.getTerminologyId(semanticTypeComponent2))) {
          throw new Exception(
              "Update cannot be used to change object identity.");
        }
      } else {
        // set semanticTypeComponent id on update
        semanticTypeComponent.setTerminologyId(idHandler
            .getTerminologyId(semanticTypeComponent));
      }
    }
    // update component
    this.updateComponent(semanticTypeComponent);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.semanticTypeChanged(semanticTypeComponent,
            WorkflowListener.Action.UPDATE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#removeSemanticTypeComponent
   * (java.lang.Long)
   */
  @Override
  public void removeSemanticTypeComponent(Long id) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - remove semanticTypeComponent " + id);
    // Remove the component
    SemanticTypeComponent semanticTypeComponent =
        removeComponent(id, SemanticTypeComponentJpa.class);

    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.semanticTypeChanged(semanticTypeComponent,
            WorkflowListener.Action.REMOVE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getDescriptor(java.lang.Long)
   */
  @Override
  public Descriptor getDescriptor(Long id) throws Exception {
    Logger.getLogger(getClass())
        .debug("Content Service - get descriptor " + id);
    Descriptor c = manager.find(DescriptorJpa.class, id);
    return c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getDescriptors(java.lang.String
   * , java.lang.String, java.lang.String)
   */
  @Override
  public DescriptorList getDescriptors(String terminologyId,
    String terminology, String version) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get descriptors " + terminologyId + "/"
            + terminology + "/" + version);
    javax.persistence.Query query =
        manager.createQuery("select c from DescriptorJpa c where "
            + "terminologyId = :terminologyId "
            + "and terminologyVersion = :version "
            + "and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      @SuppressWarnings("unchecked")
      List<Descriptor> m = query.getResultList();
      DescriptorListJpa descriptorList = new DescriptorListJpa();
      descriptorList.setObjects(m);
      descriptorList.setTotalCount(m.size());
      return descriptorList;

    } catch (NoResultException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getDescriptor(java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Descriptor getDescriptor(String terminologyId, String terminology,
    String version, String branch) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get descriptor " + terminologyId + "/" + terminology
            + "/" + version + "/" + branch);
    assert branch != null;
    DescriptorList list = getDescriptors(terminologyId, terminology, version);
    if (list == null || list.getTotalCount() == 0) {
      Logger.getLogger(getClass()).debug("  no descriptor ");
      return null;
    }
    // Find the one matching the branch (or without having been branched to)
    
    for (Descriptor d : list.getObjects()) {
      if (d.getBranch().equals(branch)) {
        return d;
      }
      
    }
    // TODO: deal with branching
    // If nothing found, return null;
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#addDescriptor(com.wci.umls.
   * server.model.content.Descriptor)
   */
  @Override
  public Descriptor addDescriptor(Descriptor descriptor) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - add descriptor " + descriptor.getTerminologyId());
    // Assign id
    IdentifierAssignmentHandler idHandler = null;
    if (assignIdentifiersFlag) {
      idHandler = idHandlerMap.get(descriptor.getTerminology());
      if (idHandler == null) {
        throw new Exception("Unable to find id handler for "
            + descriptor.getTerminology());
      }
      String id = idHandler.getTerminologyId(descriptor);
      descriptor.setTerminologyId(id);
    }

    // Add component
    Descriptor newDescriptor = addComponent(descriptor);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.descriptorChanged(newDescriptor, WorkflowListener.Action.ADD);
      }
    }
    return newDescriptor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#updateDescriptor(com.wci.umls
   * .server.model.content.Descriptor)
   */
  @Override
  public void updateDescriptor(Descriptor descriptor) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - update descriptor " + descriptor.getTerminologyId());

    // Id assignment should not change
    final IdentifierAssignmentHandler idHandler =
        idHandlerMap.get(descriptor.getTerminology());
    if (assignIdentifiersFlag) {
      if (!idHandler.allowIdChangeOnUpdate()) {
        Descriptor descriptor2 = getDescriptor(descriptor.getId());
        if (!idHandler.getTerminologyId(descriptor).equals(
            idHandler.getTerminologyId(descriptor2))) {
          throw new Exception(
              "Update cannot be used to change object identity.");
        }
      } else {
        // set descriptor id on update
        descriptor.setTerminologyId(idHandler.getTerminologyId(descriptor));
      }
    }
    // update component
    this.updateComponent(descriptor);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.descriptorChanged(descriptor, WorkflowListener.Action.UPDATE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#removeDescriptor(java.lang.
   * Long)
   */
  @Override
  public void removeDescriptor(Long id) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - remove descriptor " + id);
    // Remove the component
    Descriptor descriptor = removeComponent(id, DescriptorJpa.class);

    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.descriptorChanged(descriptor, WorkflowListener.Action.REMOVE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.services.ContentService#getCode(java.lang.Long)
   */
  @Override
  public Code getCode(Long id) throws Exception {
    Logger.getLogger(getClass()).debug("Content Service - get code " + id);
    Code c = manager.find(CodeJpa.class, id);
    return c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.services.ContentService#getCodes(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public CodeList getCodes(String terminologyId, String terminology,
    String version) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get codes " + terminologyId + "/" + terminology
            + "/" + version);
    javax.persistence.Query query =
        manager.createQuery("select c from CodeJpa c "
            + "where terminologyId = :terminologyId "
            + "and terminologyVersion = :version "
            + "and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      @SuppressWarnings("unchecked")
      List<Code> m = query.getResultList();
      CodeListJpa codeList = new CodeListJpa();
      codeList.setObjects(m);
      codeList.setTotalCount(m.size());
      return codeList;

    } catch (NoResultException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.services.ContentService#getCode(java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Code getCode(String terminologyId, String terminology, String version,
    String branch) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get code " + terminologyId + "/" + terminology + "/"
            + version + "/" + branch);
    assert branch != null;
    CodeList list = getCodes(terminologyId, terminology, version);
    if (list == null || list.getTotalCount() == 0) {
      Logger.getLogger(getClass()).debug("  no code ");
      return null;
    }
    // Find the one matching the branch (or without having been branched to)
    for (Code c : list.getObjects()) {
      if (c.getBranch().equals(branch)) {
        return c;
      }
    }
    //TODOO: deal with branching
    // If nothing found, return null;
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#addCode(com.wci.umls.server
   * .model.content.Code)
   */
  @Override
  public Code addCode(Code code) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - add code " + code.getTerminologyId());
    // Assign id
    IdentifierAssignmentHandler idHandler = null;
    if (assignIdentifiersFlag) {
      idHandler = idHandlerMap.get(code.getTerminology());
      if (idHandler == null) {
        throw new Exception("Unable to find id handler for "
            + code.getTerminology());
      }
      String id = idHandler.getTerminologyId(code);
      code.setTerminologyId(id);
    }

    // Add component
    Code newCode = addComponent(code);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.codeChanged(newCode, WorkflowListener.Action.ADD);
      }
    }
    return newCode;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#updateCode(com.wci.umls.server
   * .model.content.Code)
   */
  @Override
  public void updateCode(Code code) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - update code " + code.getTerminologyId());

    // Id assignment should not change
    final IdentifierAssignmentHandler idHandler =
        idHandlerMap.get(code.getTerminology());
    if (assignIdentifiersFlag) {
      if (!idHandler.allowIdChangeOnUpdate()) {
        Code code2 = getCode(code.getId());
        if (!idHandler.getTerminologyId(code).equals(
            idHandler.getTerminologyId(code2))) {
          throw new Exception(
              "Update cannot be used to change object identity.");
        }
      } else {
        // set code id on update
        code.setTerminologyId(idHandler.getTerminologyId(code));
      }
    }
    // update component
    this.updateComponent(code);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.codeChanged(code, WorkflowListener.Action.UPDATE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.services.ContentService#removeCode(java.lang.Long)
   */
  @Override
  public void removeCode(Long id) throws Exception {
    Logger.getLogger(getClass()).debug("Content Service - remove code " + id);
    // Remove the component
    Code code = removeComponent(id, CodeJpa.class);

    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.codeChanged(code, WorkflowListener.Action.REMOVE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getLexicalClass(java.lang.Long)
   */
  @Override
  public LexicalClass getLexicalClass(Long id) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get lexical class " + id);
    LexicalClass c = manager.find(LexicalClassJpa.class, id);
    return c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getLexicalClasss(java.lang.
   * String, java.lang.String, java.lang.String)
   */
  @Override
  public LexicalClassList getLexicalClasses(String terminologyId,
    String terminology, String version) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get lexical classs " + terminologyId + "/"
            + terminology + "/" + version);
    javax.persistence.Query query =
        manager.createQuery("select c from LexicalClassJpa c "
            + "where terminologyId = :terminologyId "
            + "and terminologyVersion = :version "
            + "and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      @SuppressWarnings("unchecked")
      List<LexicalClass> m = query.getResultList();
      LexicalClassListJpa lexicalClassList = new LexicalClassListJpa();
      lexicalClassList.setObjects(m);
      lexicalClassList.setTotalCount(m.size());
      return lexicalClassList;

    } catch (NoResultException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getLexicalClass(java.lang.String
   * , java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public LexicalClass getLexicalClass(String terminologyId, String terminology,
    String version, String branch) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get lexical class " + terminologyId + "/"
            + terminology + "/" + version + "/" + branch);
    assert branch != null;
    LexicalClassList ll =
        getLexicalClasses(terminologyId, terminology, version);
    if (ll == null || ll.getTotalCount() == 0) {
      Logger.getLogger(getClass()).debug("  no lexicalClass ");
      return null;
    }
   
    for (LexicalClass lui : ll.getObjects()) {
      if (lui.getBranch().equals(branch)) {
        return lui;
      }
    }
    // TDOO: deal with branching
    // If nothing found, return null;
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#addLexicalClass(com.wci.umls
   * .server.model.content.LexicalClass)
   */
  @Override
  public LexicalClass addLexicalClass(LexicalClass lexicalClass)
    throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - add lexical class "
            + lexicalClass.getTerminologyId());
    // Assign id
    IdentifierAssignmentHandler idHandler = null;
    if (assignIdentifiersFlag) {
      idHandler = idHandlerMap.get(lexicalClass.getTerminology());
      if (idHandler == null) {
        throw new Exception("Unable to find id handler for "
            + lexicalClass.getTerminology());
      }
      String id = idHandler.getTerminologyId(lexicalClass);
      lexicalClass.setTerminologyId(id);
    }

    // Add component
    LexicalClass newLexicalClass = addComponent(lexicalClass);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.lexicalClassChanged(newLexicalClass,
            WorkflowListener.Action.ADD);
      }
    }
    return newLexicalClass;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#updateLexicalClass(com.wci.
   * umls.server.model.content.LexicalClass)
   */
  @Override
  public void updateLexicalClass(LexicalClass lexicalClass) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - update lexical class "
            + lexicalClass.getTerminologyId());

    // Id assignment should not change
    final IdentifierAssignmentHandler idHandler =
        idHandlerMap.get(lexicalClass.getTerminology());
    if (assignIdentifiersFlag) {
      if (!idHandler.allowIdChangeOnUpdate()) {
        LexicalClass lexicalClass2 = getLexicalClass(lexicalClass.getId());
        if (!idHandler.getTerminologyId(lexicalClass).equals(
            idHandler.getTerminologyId(lexicalClass2))) {
          throw new Exception(
              "Update cannot be used to change object identity.");
        }
      } else {
        // set lexicalClass id on update
        lexicalClass.setTerminologyId(idHandler.getTerminologyId(lexicalClass));
      }
    }
    // update component
    this.updateComponent(lexicalClass);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.lexicalClassChanged(lexicalClass,
            WorkflowListener.Action.UPDATE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#removeLexicalClass(java.lang
   * .Long)
   */
  @Override
  public void removeLexicalClass(Long id) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - remove lexical class " + id);
    // Remove the component
    LexicalClass lexicalClass = removeComponent(id, LexicalClassJpa.class);

    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.lexicalClassChanged(lexicalClass,
            WorkflowListener.Action.REMOVE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getStringClass(java.lang.Long)
   */
  @Override
  public StringClass getStringClass(Long id) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get string class " + id);
    StringClass c = manager.find(StringClassJpa.class, id);
    return c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getStringClasss(java.lang.String
   * , java.lang.String, java.lang.String)
   */
  @Override
  public StringClassList getStringClasses(String terminologyId,
    String terminology, String version) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get string classs " + terminologyId + "/"
            + terminology + "/" + version);
    javax.persistence.Query query =
        manager.createQuery("select c from StringClassJpa c "
            + "where terminologyId = :terminologyId "
            + "and terminologyVersion = :version "
            + "and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      @SuppressWarnings("unchecked")
      List<StringClass> m = query.getResultList();
      StringClassListJpa stringClassList = new StringClassListJpa();
      stringClassList.setObjects(m);
      stringClassList.setTotalCount(m.size());
      return stringClassList;

    } catch (NoResultException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getStringClass(java.lang.String
   * , java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public StringClass getStringClass(String terminologyId, String terminology,
    String version, String branch) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - get string class " + terminologyId + "/"
            + terminology + "/" + version + "/" + branch);
    assert branch != null;
    StringClassList list =
        getStringClasses(terminologyId, terminology, version);
    if (list == null || list.getTotalCount() == 0) {
      Logger.getLogger(getClass()).debug("  no string class ");
      return null;
    }
    // Find the one matching the branch (or without having been branched to)
    for (StringClass sui : list.getObjects()) {
      if (sui.getBranch().equals(branch)) {
        return sui;
      }
    }
// TODO:deal with branching
    // If nothing found, return null;
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#addStringClass(com.wci.umls
   * .server.model.content.StringClass)
   */
  @Override
  public StringClass addStringClass(StringClass stringClass) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - add string class " + stringClass.getTerminologyId());
    // Assign id
    IdentifierAssignmentHandler idHandler = null;
    if (assignIdentifiersFlag) {
      idHandler = idHandlerMap.get(stringClass.getTerminology());
      if (idHandler == null) {
        throw new Exception("Unable to find id handler for "
            + stringClass.getTerminology());
      }
      String id = idHandler.getTerminologyId(stringClass);
      stringClass.setTerminologyId(id);
    }

    // Add component
    StringClass newStringClass = addComponent(stringClass);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener
            .stringClassChanged(newStringClass, WorkflowListener.Action.ADD);
      }
    }
    return newStringClass;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#updateStringClass(com.wci.umls
   * .server.model.content.StringClass)
   */
  @Override
  public void updateStringClass(StringClass stringClass) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - update string class "
            + stringClass.getTerminologyId());

    // Id assignment should not change
    final IdentifierAssignmentHandler idHandler =
        idHandlerMap.get(stringClass.getTerminology());
    if (assignIdentifiersFlag) {
      if (!idHandler.allowIdChangeOnUpdate()) {
        StringClass stringClass2 = getStringClass(stringClass.getId());
        if (!idHandler.getTerminologyId(stringClass).equals(
            idHandler.getTerminologyId(stringClass2))) {
          throw new Exception(
              "Update cannot be used to change object identity.");
        }
      } else {
        // set stringClass id on update
        stringClass.setTerminologyId(idHandler.getTerminologyId(stringClass));
      }
    }
    // update component
    this.updateComponent(stringClass);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener
            .stringClassChanged(stringClass, WorkflowListener.Action.UPDATE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#removeStringClass(java.lang
   * .Long)
   */
  @Override
  public void removeStringClass(Long id) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - remove string class " + id);
    // Remove the component
    StringClass stringClass = removeComponent(id, StringClassJpa.class);

    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener
            .stringClassChanged(stringClass, WorkflowListener.Action.REMOVE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#findDescendantConcepts(com.
   * wci.umls.server.model.content.Concept, boolean,
   * com.wci.umls.server.helpers.PfsParameter)
   */
  @Override
  public ConceptList findDescendantConcepts(Concept concept,
    boolean parentsOnly, PfsParameter pfsParameter) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#findAncestorConcepts(com.wci
   * .umls.server.model.content.Concept, boolean,
   * com.wci.umls.server.helpers.PfsParameter)
   */
  @Override
  public ConceptList findAncestorConcepts(Concept concept,
    boolean childrenOnly, PfsParameter pfsParameter) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#findDescendantDescriptors(com
   * .wci.umls.server.model.content.Descriptor, boolean,
   * com.wci.umls.server.helpers.PfsParameter)
   */
  @Override
  public DescriptorList findDescendantDescriptors(Descriptor descriptor,
    boolean parentsOnly, PfsParameter pfsParameter) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#findAncestorDescriptors(com
   * .wci.umls.server.model.content.Descriptor, boolean,
   * com.wci.umls.server.helpers.PfsParameter)
   */
  @Override
  public DescriptorList findAncestorDescriptors(Descriptor descriptor,
    boolean childrenOnly, PfsParameter pfsParameter) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#findDescendantCodes(com.wci
   * .umls.server.model.content.Code, boolean,
   * com.wci.umls.server.helpers.PfsParameter)
   */
  @Override
  public CodeList findDescendantCodes(Code code, boolean parentsOnly,
    PfsParameter pfsParameter) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#findAncestorCodes(com.wci.umls
   * .server.model.content.Code, boolean,
   * com.wci.umls.server.helpers.PfsParameter)
   */
  @Override
  public CodeList findAncestorCodes(Code code, boolean childrenOnly,
    PfsParameter pfsParameter) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.services.ContentService#getAtom(java.lang.Long)
   */
  @Override
  public Atom getAtom(Long id) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.wci.umls.server.services.ContentService#getAtom(java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Atom getAtom(String terminologyId, String terminology, String version,
    String branch) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#addAtom(com.wci.umls.server
   * .model.content.Atom)
   */
  @Override
  public Atom addAtom(Atom atom) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - add atom " + atom.getTerminologyId());
    // Assign id
    IdentifierAssignmentHandler idHandler = null;
    if (assignIdentifiersFlag) {
      idHandler = idHandlerMap.get(atom.getTerminology());
      if (idHandler == null) {
        throw new Exception("Unable to find id handler for "
            + atom.getTerminology());
      }
      atom.setTerminologyId(idHandler.getTerminologyId(atom));
    }
    if (assignIdentifiersFlag && idHandler == null) {
      throw new Exception("Unable to find id handler for "
          + atom.getTerminology());
    }

    // Add component
    Atom newAtom = addComponent(atom);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.atomChanged(newAtom, WorkflowListener.Action.ADD);
      }
    }
    return newAtom;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ContentService#updateAtom(org.ihtsdo
   * .otf.mapping.rf2.Atom)
   */
  @Override
  public void updateAtom(Atom atom) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Content Service - update atom " + atom.getTerminologyId());
    // Id assignment
    final IdentifierAssignmentHandler idHandler =
        idHandlerMap.get(atom.getTerminology());
    if (!idHandler.allowIdChangeOnUpdate() && assignIdentifiersFlag) {
      Atom atom2 = getAtom(atom.getId());
      if (!idHandler.getTerminologyId(atom).equals(
          idHandler.getTerminologyId(atom2))) {
        throw new Exception("Update cannot be used to change object identity.");
      }
    }

    // update component
    this.updateComponent(atom);

    // Inform listeners
    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.atomChanged(atom, WorkflowListener.Action.UPDATE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ContentService#removeAtom(java.lang
   * .String)
   */
  @Override
  public void removeAtom(Long id) throws Exception {
    Logger.getLogger(getClass()).debug("Content Service - remove atom " + id);
    // Remove the component
    Atom atom = removeComponent(id, AtomJpa.class);

    if (listenersEnabled) {
      for (WorkflowListener listener : listeners) {
        listener.atomChanged(atom, WorkflowListener.Action.REMOVE);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#addRelationship(com.wci.umls
   * .server.model.content.Relationship)
   */
  @Override
  public Relationship<? extends ComponentHasAttributes, ? extends ComponentHasAttributes> addRelationship(
    Relationship<? extends ComponentHasAttributes, ? extends ComponentHasAttributes> relationship)
    throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#updateRelationship(com.wci.
   * umls.server.model.content.Relationship)
   */
  @Override
  public void updateRelationship(
    Relationship<? extends ComponentHasAttributes, ? extends ComponentHasAttributes> relationship)
    throws Exception {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#removeRelationship(java.lang
   * .Long)
   */
  @Override
  public void removeRelationship(Long id) throws Exception {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#addTransitiveRelationship(com
   * .wci.umls.server.model.content.TransitiveRelationship)
   */
  @Override
  public TransitiveRelationship<? extends ComponentHasAttributes> addTransitiveRelationship(
    TransitiveRelationship<? extends ComponentHasAttributes> transitiveRelationship)
    throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#updateTransitiveRelationship
   * (com.wci.umls.server.model.content.TransitiveRelationship)
   */
  @Override
  public void updateTransitiveRelationship(
    TransitiveRelationship<? extends ComponentHasAttributes> transitiveRelationship)
    throws Exception {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#removeTransitiveRelationship
   * (java.lang.Long)
   */
  @Override
  public void removeTransitiveRelationship(Long id) throws Exception {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#findConceptsForQuery(java.lang
   * .String, java.lang.String, java.lang.String,
   * com.wci.umls.server.helpers.PfsParameter)
   */
  @Override
  public SearchResultList findConceptsForQuery(String terminology,
    String version, String query, PfsParameter pfs) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#findConceptsForSearchCriteria
   * (java.lang.String, java.lang.String, java.lang.String,
   * com.wci.umls.server.helpers.SearchCriteriaList,
   * com.wci.umls.server.helpers.PfsParameter)
   */
  @Override
  public SearchResultList findConceptsForSearchCriteria(String terminology,
    String version, String query, SearchCriteriaList criteria, PfsParameter pfs)
    throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getAllConcepts(java.lang.String
   * , java.lang.String)
   */
  @Override
  public ConceptList getAllConcepts(String terminology, String version,
    String branch) {
    Logger.getLogger(getClass()).debug(
        "Content Service - get all concepts " + terminology + "/" + version
            + "/" + branch);
    assert branch != null;

    try {
      javax.persistence.Query query =
          manager.createQuery("select c from ConceptJpa c "
              + "where terminologyVersion = :version "
              + "and terminology = :terminology ");
      // TODO: implement branching
      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      @SuppressWarnings("unchecked")
      List<Concept> concepts = query.getResultList();
      ConceptList conceptList = new ConceptListJpa();
      conceptList.setObjects(concepts);
      conceptList.setTotalCount(concepts.size());
      return conceptList;
    } catch (NoResultException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getAllDescriptors(java.lang
   * .String, java.lang.String, java.lang.String)
   */
  @Override
  public DescriptorList getAllDescriptors(String terminology, String version,
    String branch) {
    Logger.getLogger(getClass()).debug(
        "Content Service - get all descriptors " + terminology + "/" + version
            + "/" + branch);
    assert branch != null;

    try {
      javax.persistence.Query query =
          manager.createQuery("select c from DescriptorJpa c "
              + "where terminologyVersion = :version "
              + "and terminology = :terminology ");

      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      // TODO: deal with branching

      @SuppressWarnings("unchecked")
      List<Descriptor> descriptors = query.getResultList();
      DescriptorList descriptorList = new DescriptorListJpa();
      descriptorList.setObjects(descriptors);
      descriptorList.setTotalCount(descriptors.size());
      return descriptorList;
    } catch (NoResultException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getAllCodes(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public CodeList getAllCodes(String terminology, String version, String branch) {
    Logger.getLogger(getClass()).debug(
        "Content Service - get all codes " + terminology + "/" + version + "/"
            + branch);
    assert branch != null;

    try {
      javax.persistence.Query query =
          manager.createQuery("select c from CodeJpa c "
              + "where terminologyVersion = :version "
              + "and terminology = :terminology ");

      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      // TODO: deal with branching
      @SuppressWarnings("unchecked")
      List<Code> codes = query.getResultList();
      CodeList codeList = new CodeListJpa();
      codeList.setObjects(codes);
      codeList.setTotalCount(codes.size());
      return codeList;
    } catch (NoResultException e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#clearTransitiveClosure(java
   * .lang.String, java.lang.String)
   */
  @Override
  public void clearTransitiveClosure(String terminology, String version)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "Content Service - Clear transitive closure data for " + terminology
            + ", " + version);
    try {
      if (getTransactionPerOperation()) {
        // remove simple ref set member
        tx.begin();
      }

      javax.persistence.Query query =
          manager.createQuery("DELETE From ConceptTransitiveRelationshipJpa "
              + " c where terminology = :terminology "
              + " and terminologyVersion = :version");
      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      int deleteRecords = query.executeUpdate();
      Logger.getLogger(getClass()).info(
          "    ConceptTransitiveRelationshipJpa records deleted = "
              + deleteRecords);

      query =
          manager
              .createQuery("DELETE From DescriptorTransitiveRelationshipJpa "
                  + " c where terminology = :terminology "
                  + " and terminologyVersion = :version");
      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      deleteRecords = query.executeUpdate();
      Logger.getLogger(getClass()).info(
          "    DescriptorTransitiveRelationshipJpa records deleted = "
              + deleteRecords);

      query =
          manager.createQuery("DELETE From CodeTransitiveRelationshipJpa "
              + " c where terminology = :terminology "
              + " and terminologyVersion = :version");
      query.setParameter("terminology", terminology);
      query.setParameter("version", version);
      deleteRecords = query.executeUpdate();
      Logger.getLogger(getClass()).info(
          "    CodeTransitiveRelationshipJpa records deleted = "
              + deleteRecords);

      if (getTransactionPerOperation()) {
        // remove simple ref set member
        tx.commit();
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#clearConcepts(java.lang.String,
   * java.lang.String)
   */
  @Override
  public void clearConcepts(String terminology, String version) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#clearBranch(java.lang.String)
   */
  @Override
  public void clearBranch(String branch) {
    // TODO
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getGraphResolutionHandler(java
   * .lang.String)
   */
  @Override
  public GraphResolutionHandler getGraphResolutionHandler(String terminology)
    throws Exception {
    if (graphResolverMap.containsKey(terminology)) {
      return graphResolverMap.get(terminology);
    }
    return graphResolverMap.get("DEFAULT");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getIdentifierAssignmentHandler
   * (java.lang.String)
   */
  @Override
  public IdentifierAssignmentHandler getIdentifierAssignmentHandler(
    String terminology) throws Exception {
    if (idHandlerMap.containsKey(terminology)) {
      return idHandlerMap.get(terminology);
    }
    return idHandlerMap.get("DEFAULT");

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getComputePreferredNameHandler
   * (java.lang.String)
   */
  @Override
  public ComputePreferredNameHandler getComputePreferredNameHandler(
    String terminology) throws Exception {
    if (pnHandlerMap.containsKey(terminology)) {
      return pnHandlerMap.get(terminology);
    }
    return pnHandlerMap.get("DEFAULT");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.wci.umls.server.services.ContentService#getComputedPreferredName(com
   * .wci.umls.server.model.content.Concept)
   */
  @Override
  public String getComputedPreferredName(AtomClass atomClass) throws Exception {
    try {
      ComputePreferredNameHandler handler =
          pnHandlerMap.get(atomClass.getTerminology());
      // look for default if null
      if (handler == null) {
        handler = pnHandlerMap.get("DEFAULT");
      }
      if (handler == null) {
        throw new Exception(
            "Compute preferred name handler is not configured for DEFAULT or for "
                + atomClass.getTerminology());
      }
      final String pn = handler.computePreferredName(atomClass.getAtoms());
      return pn;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.services.ContentService#isLastModifiedFlag()
   */
  @Override
  public boolean isLastModifiedFlag() {
    return lastModifiedFlag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.ts.services.ContentService#setLastModifiedFlag(boolean)
   */
  @Override
  public void setLastModifiedFlag(boolean lastModifiedFlag) {
    this.lastModifiedFlag = lastModifiedFlag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.ts.services.ContentService#setAssignIdentifiersFlag(boolean)
   */
  @Override
  public void setAssignIdentifiersFlag(boolean assignIdentifiersFlag) {
    this.assignIdentifiersFlag = assignIdentifiersFlag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.ts.services.ContentService#getContentStats(java.lang.String,
   * java.lang.String)
   */
  @Override
  public Map<String, Integer> getComponentStats(String terminology,
    String version, String branch) throws Exception {
    Logger.getLogger(getClass()).info("Content Service - getComponentStats");
    assert branch != null;
    Map<String, Integer> stats = new HashMap<>();
    for (EntityType<?> type : manager.getMetamodel().getEntities()) {
      String jpaTable = type.getName();
      // Logger.getLogger(getClass()).debug("  jpaTable = " + jpaTable);
      // Skip audit trail tables
      if (jpaTable.toUpperCase().indexOf("_AUD") != -1) {
        continue;
      }
      if (!AbstractAbbreviation.class.isAssignableFrom(type
          .getBindableJavaType())
          && !AbstractComponentHasAttributes.class.isAssignableFrom(type
              .getBindableJavaType())) {
        continue;
      }
      Logger.getLogger(getClass()).info("  " + jpaTable);
      javax.persistence.Query query = null;
      // TODO, deal with branching
      if (terminology != null) {
          query =
              manager
                  .createQuery("select count(*) from " + jpaTable
                      + " where terminology = :terminology "
                      + "and terminologyVersion = :version ");
          query.setParameter("terminology", terminology);
          query.setParameter("version", version);
        
      } else {
          query =
              manager.createQuery("select count(*) from " + jpaTable);
      }
      int ct = ((Long) query.getSingleResult()).intValue();
      stats.put("Total " + jpaTable, ct);

      // Only compute active counts for components
      if (AbstractComponentHasAttributes.class.isAssignableFrom(type
          .getBindableJavaType())) {
        // TODO: deal with branching
        if (terminology != null) {
         
            query =
                manager.createQuery("select count(*) from " + jpaTable
                    + " where obsolete = 0 and terminology = :terminology "
                    + "and terminologyVersion = :version ");
            query.setParameter("terminology", terminology);
            query.setParameter("version", version);
        } else {
            query =
                manager.createQuery("select count(*) from " + jpaTable);
        }
        ct = ((Long) query.getSingleResult()).intValue();
        stats.put("Non-obsolete " + jpaTable, ct);
      }
    }
    return stats;
  }

  /**
   * Adds the component.
   *
   * @param <T> the
   * @param component the component
   * @return the t
   * @throws Exception the exception
   */
  private <T extends Component> T addComponent(T component) throws Exception {
    try {
      // Set last modified date
      if (lastModifiedFlag) {
        component.setLastModified(new Date());
      }

      // add
      if (getTransactionPerOperation()) {
        tx = manager.getTransaction();
        tx.begin();
        manager.persist(component);
        tx.commit();
      } else {
        manager.persist(component);
      }
      return component;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /**
   * Update component.
   *
   * @param <T> the generic type
   * @param component the component
   * @throws Exception the exception
   */
  private <T extends Component> void updateComponent(T component)
    throws Exception {
    try {
      // Set modification date
      if (lastModifiedFlag) {
        component.setLastModified(new Date());
      }

      // update
      if (getTransactionPerOperation()) {
        tx = manager.getTransaction();
        tx.begin();
        manager.merge(component);
        tx.commit();
      } else {
        manager.merge(component);
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }

  }

  /**
   * Returns the component.
   *
   * @param <T> the
   * @param id the id
   * @param clazz the clazz
   * @return the component
   * @throws Exception the exception
   */
  private <T extends Component> T getComponent(Long id, Class<T> clazz)
    throws Exception {
    // Get transaction and object
    tx = manager.getTransaction();
    T component = manager.find(clazz, id);
    return component;
  }

  /**
   * Removes the component.
   *
   * @param <T> the generic type
   * @param id the id
   * @param clazz the clazz
   * @return the component
   * @throws Exception the exception
   */
  private <T extends Component> T removeComponent(Long id, Class<T> clazz)
    throws Exception {
    try {
      // Get transaction and object
      tx = manager.getTransaction();
      T component = manager.find(clazz, id);

      // Set modification date
      if (lastModifiedFlag) {
        component.setLastModified(new Date());
      }

      // Remove
      if (getTransactionPerOperation()) {
        // remove refset member
        tx.begin();
        if (manager.contains(component)) {
          manager.remove(component);
        } else {
          manager.remove(manager.merge(component));
        }
        tx.commit();
      } else {
        if (manager.contains(component)) {
          manager.remove(component);
        } else {
          manager.remove(manager.merge(component));
        }
      }
      return component;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

}
