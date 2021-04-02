/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.maint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.persistence.Query;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.jpa.content.AtomRelationshipJpa;
import com.wci.umls.server.jpa.content.AtomSubsetJpa;
import com.wci.umls.server.jpa.content.AtomSubsetMemberJpa;
import com.wci.umls.server.jpa.content.AttributeJpa;
import com.wci.umls.server.jpa.content.CodeJpa;
import com.wci.umls.server.jpa.content.CodeRelationshipJpa;
import com.wci.umls.server.jpa.content.ComponentHistoryJpa;
import com.wci.umls.server.jpa.content.ComponentInfoRelationshipJpa;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.jpa.content.ConceptRelationshipJpa;
import com.wci.umls.server.jpa.content.ConceptSubsetJpa;
import com.wci.umls.server.jpa.content.ConceptSubsetMemberJpa;
import com.wci.umls.server.jpa.content.DefinitionJpa;
import com.wci.umls.server.jpa.content.DescriptorJpa;
import com.wci.umls.server.jpa.content.DescriptorRelationshipJpa;
import com.wci.umls.server.jpa.content.GeneralConceptAxiomJpa;
import com.wci.umls.server.jpa.content.LexicalClassJpa;
import com.wci.umls.server.jpa.content.MapSetJpa;
import com.wci.umls.server.jpa.content.MappingJpa;
import com.wci.umls.server.jpa.content.SemanticTypeComponentJpa;
import com.wci.umls.server.jpa.content.StringClassJpa;
import com.wci.umls.server.model.content.Component;

/**
 * Implementation of an algorithm to update publishable but not published to
 * published
 */
public class UpdatePublishedAlgorithm extends AbstractInsertMaintReleaseAlgorithm {

  /**
   * Instantiates an empty {@link UpdatePublishedAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public UpdatePublishedAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("UPDATEPUBLISHED");
    setLastModifiedBy("admin");
  }

  /**
   * Check preconditions.
   *
   * @return the validation result
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    ValidationResult validationResult = new ValidationResultJpa();

    if (getProject() == null) {
      throw new Exception("Update Published requires a project to be set");
    }

    return validationResult;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings({
      "rawtypes", "unchecked"
  })
  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    try {

      logInfo("[Update Published] Setting publishable and not published components to published.");
      commitClearBegin();

      // Find all publishable and not published components and abbreviations
      final List<Class> componentClassList =
          new ArrayList<>(Arrays.asList(AtomRelationshipJpa.class, AtomSubsetMemberJpa.class,
              AtomSubsetJpa.class, AtomJpa.class, AttributeJpa.class, CodeRelationshipJpa.class,
              CodeJpa.class, ComponentHistoryJpa.class, ComponentInfoRelationshipJpa.class,
              ConceptRelationshipJpa.class, ConceptSubsetMemberJpa.class, ConceptSubsetJpa.class,
              ConceptJpa.class, DefinitionJpa.class, DescriptorRelationshipJpa.class,
              DescriptorJpa.class, GeneralConceptAxiomJpa.class, LexicalClassJpa.class,
              MappingJpa.class, MapSetJpa.class, SemanticTypeComponentJpa.class,
              StringClassJpa.class));

      setSteps(componentClassList.size());

      for (final Class clazz : componentClassList) {
        logInfo("  Update " + clazz.getSimpleName());
        Query query = manager.createQuery("SELECT a.id FROM " + clazz.getSimpleName()
            + " a WHERE published = false AND publishable = true");
        // Add the number of objects returned to the steps, so we can log and
        // commit periodically
        setSteps(getSteps() + query.getResultList().size());

        for (final Long id : (List<Long>) query.getResultList()) {
          final Component component = getComponent(id, clazz);
          component.setPublished(true);
          updateComponent(component);
          // Update the progress
          updateProgress();
        }
        commitClearBegin();

        // Update the progress
        updateProgress();
      }

      commitClearBegin();

      logInfo("  project = " + getProject().getId());
      logInfo("  workId = " + getWorkId());
      logInfo("  activityId = " + getActivityId());
      logInfo("  user  = " + getLastModifiedBy());
      logInfo("Finished " + getName());

    } catch (

    Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      throw e;
    }

  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // n/a - No reset
    logInfo("Finished RESET " + getName());
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /**
   * Returns the parameters.
   *
   * @return the parameters
   */
  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();
    return params;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Removes contents for non-current terminologies";
  }

}