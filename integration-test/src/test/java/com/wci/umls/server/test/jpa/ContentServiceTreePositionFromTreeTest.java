/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa;

import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.content.Tree;
import com.wci.umls.server.helpers.content.TreePositionList;
import com.wci.umls.server.jpa.content.ConceptTreePositionJpa;
import com.wci.umls.server.jpa.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.model.content.ComponentHasAttributesAndName;
import com.wci.umls.server.model.content.TreePosition;
import com.wci.umls.server.services.ContentService;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Sample test to get auto complete working
 */
public class ContentServiceTreePositionFromTreeTest
    extends IntegrationUnitSupport {

  /** The service. */
  ContentService service = null;

  /**
   * Setup class.
   */
  @BeforeClass
  public static void setupClass() {
    // do nothing
  }

  /**
   * Setup.
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    service = new ContentServiceJpa();
    service.setLastModifiedBy("admin");
    service.setMolecularActionFlag(false);
  }

  /**
   * Test ability to extract a tree based on a tree position.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptTreePositionFromTree() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    // Start by obtaining tree positions for a concept
    TreePositionList list = service.findTreePositions("10944007", "SNOMEDCT_US",
        "2016_03_01", Branch.ROOT, null, ConceptTreePositionJpa.class,
        new PfsParameterJpa());

    TreePosition<? extends ComponentHasAttributesAndName> treepos =
        list.getObjects().get(0);
    Logger.getLogger(getClass()).info(" first treepos = "
        + treepos.getNode().getId() + ", " + treepos.getAncestorPath());

    Tree tree = service.getTreeForTreePosition(treepos);
    Logger.getLogger(getClass()).debug("  tree = " + tree);

    treepos = list.getObjects().get(1);
    Tree tree2 = service.getTreeForTreePosition(treepos);
    Logger.getLogger(getClass()).debug("  tree2 = " + tree2);

    tree.mergeTree(tree2, null);
    Logger.getLogger(getClass()).debug("  tree = " + tree);

    // merged tree is bigger
    assertTrue(tree.toString().length() > tree2.toString().length());

  }

  /**
   * Teardown.
   */
  @After
  public void teardown() {
    // do nothing
  }

  /**
   * Teardown class.
   */
  @AfterClass
  public static void teardownClass() {
    // do nothing
  }

}
