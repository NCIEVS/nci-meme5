/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.maint;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.wci.umls.server.jpa.model.AlgorithmParameterJpa;
import com.wci.umls.server.model.algo.AlgorithmParameter;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;

/**
 * Implementation of an algorithm to create relationships of a chosen type
 * between every concept listed in a particular cluster.
 */
public class CreateRelationshipsForCluster
    extends CreateXRRelationshipsForCluster {

  /**
   * Instantiates an empty {@link CreateRelationshipsForCluster}.
   *
   * @throws Exception if anything goes wrong
   */
  public CreateRelationshipsForCluster() throws Exception {
    super();
    setRelationshipType(null);
    setWorkId("CREATERELATIONSHIPS");
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    super.checkProperties(p);
    checkRequiredProperties(new String[] {
        "relationshipType"
    }, p);
    if (p.getProperty("relationshipType") == null
        || p.getProperty("relationshipType").trim().isEmpty()) {
      throw new Exception("Required property relationshipType missing");
    }
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    super.setProperties(p);
    if (p.getProperty("relationshipType") != null) {
      setRelationshipType(String.valueOf(p.getProperty("relationshipType")));
    }
  }

  /**
   * Returns the parameters.
   *
   * @return the parameters
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();

    final AlgorithmParameter param =
        new AlgorithmParameterJpa("Relationship Type", "relationshipType",
            "Relationship type", "e.g. RO", 10,
            AlgorithmParameter.Type.ENUM, "");
    param.setPossibleValues(getRelationshipTypeValues());
    params.add(param);

    return params;
  }

  /* see superclass */
  @Override
  protected String getRelationshipActionActivityId() {
    return "createRelationshipsForCluster";
  }

  /* see superclass */
  @Override
  protected ConceptRelationship getExistingBlockingRelationship(
    final Concept fromConcept, final Concept toConcept,
    final Map<Long, Set<Long>> existingRelationships) throws Exception {
    for (final ConceptRelationship relationship : fromConcept
        .getRelationships()) {
      if (relationship.getTo().getId().equals(toConcept.getId())
          && getRelationshipTypeToCreate()
              .equals(relationship.getRelationshipType())) {
        return relationship;
      }
    }
    return null;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Create relationships of the specified type between all concepts "
        + "listed in a cluster";
  }
}
