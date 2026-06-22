/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.wci.umls.server.jpa.model.workflow.WorkflowEpochJpa;
import com.wci.umls.server.model.workflow.WorkflowEpoch;

/**
 * Unit tests for {@link WorkflowServiceJpa}.
 */
public class WorkflowServiceJpaUnitTest {

  /**
   * Test max workflow epoch lookup by name.
   */
  @Test
  public void getMaxWorkflowEpochByNameReturnsHighestEpochName() {
    final WorkflowEpoch currentEpoch = WorkflowServiceJpa.getMaxWorkflowEpochByName(
        Arrays.asList(epoch("25b"), epoch("26a"), epoch("25a")));

    assertEquals("26a", currentEpoch.getName());
  }

  /**
   * Test empty max workflow epoch lookup.
   */
  @Test
  public void getMaxWorkflowEpochByNameReturnsNullForEmptyList() {
    assertNull(WorkflowServiceJpa.getMaxWorkflowEpochByName(Collections.emptyList()));
  }

  /**
   * Returns a workflow epoch with the specified name.
   *
   * @param name the name
   * @return the workflow epoch
   */
  private WorkflowEpoch epoch(final String name) {
    final WorkflowEpochJpa epoch = new WorkflowEpochJpa();
    epoch.setName(name);
    return epoch;
  }
}
