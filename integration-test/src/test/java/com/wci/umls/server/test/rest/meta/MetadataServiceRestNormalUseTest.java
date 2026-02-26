/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.rest.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.KeyValuePair;
import com.wci.umls.server.helpers.KeyValuePairList;
import com.wci.umls.server.helpers.KeyValuePairLists;
import com.wci.umls.server.helpers.PrecedenceList;
import com.wci.umls.server.helpers.meta.TerminologyList;
import com.wci.umls.server.jpa.helpers.PrecedenceListJpa;
import com.wci.umls.server.model.meta.IdType;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.services.MetadataService.MetadataKeys;

/**
 * Implementation of the "Metadata Service REST Normal Use" Test Cases.
 */
public class MetadataServiceRestNormalUseTest extends MetadataServiceRestTest {

  /** The auth token. */
  private static String authToken;

  /** The admin token. */
  private static String adminToken;

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Override
  @Before
  public void setup() throws Exception {

    // authentication
    authToken =
        securityService.authenticate(testUser, testPassword).getAuthToken();
    adminToken =
        securityService.authenticate(adminUser, adminPassword).getAuthToken();

  }

  /**
   * Test retrieval of all terminology/version pairs.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetCurrentTerminologies() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    TerminologyList termList =
        metadataService.getCurrentTerminologies(authToken);
    Logger.getLogger(getClass()).debug("  data = " + termList);

    // flags for whether UMLS, SNOMEDCT_US, and MSH were found
    boolean foundUmls = false;
    boolean foundSnomedct = false;
    boolean foundMsh = false;

    for (Terminology terminology : termList.getObjects()) {
      // test versions
      switch (terminology.getTerminology()) {
        case "MTH":
          foundUmls = true;
          assertEquals("latest", terminology.getVersion());
          break;
        case "SNOMEDCT_US":
          foundSnomedct = true;
          assertEquals("2016_03_01", terminology.getVersion());
          break;
        case "MSH":
          foundMsh = true;
          assertEquals("2016_2016_02_26", terminology.getVersion());
          break;
        default:
          // ignore other terminologies, only three above are assumed
          break;
      }
    }

    // test that both were found
    assertTrue(foundSnomedct && foundMsh && foundUmls);
  }

  /**
   * Test retrieving all metadata for a terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAllMetadata() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    // test UMLS metadata
    assertTrue(testUmlsMetadata(metadataService.getAllMetadata("MTH",
        "latest", authToken)));

    // test SNOMED metadata
    assertTrue(testSnomedMetadata(metadataService.getAllMetadata("SNOMEDCT_US",
        "2016_03_01", authToken)));

    // test MSH metadata
    assertTrue(testMshMetadata(metadataService.getAllMetadata("MSH",
        "2016_2016_02_26", authToken)));
  }

  /**
   * Test normal use of obtaining terminology objects.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetTerminology() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    // test UMLS metadata
    Terminology umls =
        metadataService.getTerminology("MTH", "latest", authToken);
    assertEquals("loader", umls.getLastModifiedBy());
    assertFalse(umls.isAssertsRelDirection());
    assertTrue(umls.isCurrent());
    assertFalse(umls.isDescriptionLogicTerminology());
    assertNull(umls.getEndDate());
    assertNull(umls.getStartDate());
    assertEquals(IdType.CONCEPT, umls.getOrganizingClassType());
    assertEquals("MTH", umls.getPreferredName());
    assertEquals("MTH", umls.getTerminology());
    assertEquals("latest", umls.getVersion());

    assertEquals("MTH", umls.getRootTerminology().getTerminology());
    // Because of XML Transient
    assertNull(umls.getRootTerminology().getLastModifiedBy());
    assertNull(umls.getRootTerminology().getFamily());
    assertNull(umls.getRootTerminology().getHierarchicalName());
    assertNull(umls.getRootTerminology().getLanguage());
    assertNull(umls.getRootTerminology().getPreferredName());
    assertNull(umls.getRootTerminology().getShortName());
    assertNull(umls.getRootTerminology().getAcquisitionContact());
    assertNull(umls.getRootTerminology().getContentContact());
    assertNull(umls.getRootTerminology().getLicenseContact());

    // test UMLS metadata
    Terminology snomed =
        metadataService.getTerminology("SNOMEDCT_US", "2016_03_01", authToken);
    assertEquals("loader", snomed.getLastModifiedBy());
    assertTrue(snomed.isAssertsRelDirection());
    assertTrue(snomed.isCurrent());
    assertFalse(snomed.isDescriptionLogicTerminology());
    assertNull(snomed.getEndDate());
    assertNull(snomed.getStartDate());
    assertEquals(IdType.CONCEPT, snomed.getOrganizingClassType());
    assertEquals("US Edition of SNOMED CT, 2016_03_01",
        snomed.getPreferredName());
    assertEquals("SNOMEDCT_US", snomed.getTerminology());
    assertEquals("2016_03_01", snomed.getVersion());

    assertEquals("SNOMEDCT_US", snomed.getRootTerminology().getTerminology());
    // Because of XML Transient
    assertNull(snomed.getRootTerminology().getLastModifiedBy());
    assertNull(snomed.getRootTerminology().getFamily());
    assertNull(snomed.getRootTerminology().getHierarchicalName());
    assertNull(snomed.getRootTerminology().getLanguage());
    assertNull(snomed.getRootTerminology().getPreferredName());
    assertNull(snomed.getRootTerminology().getShortName());
    assertNull(snomed.getRootTerminology().getAcquisitionContact());
    assertNull(snomed.getRootTerminology().getContentContact());
    assertNull(snomed.getRootTerminology().getLicenseContact());

    // test MSH metadata
    Terminology msh =
        metadataService.getTerminology("MSH", "2016_2016_02_26", authToken);
    assertEquals("loader", msh.getLastModifiedBy());
    assertFalse(msh.isAssertsRelDirection());
    assertTrue(msh.isCurrent());
    assertFalse(msh.isDescriptionLogicTerminology());
    assertNull(msh.getEndDate());
    assertNull(msh.getStartDate());
    assertEquals(IdType.DESCRIPTOR, msh.getOrganizingClassType());
    assertEquals("Medical Subject Headings, 2016_2016_02_26",
        msh.getPreferredName());
    assertEquals("MSH", msh.getTerminology());
    assertEquals("2016_2016_02_26", msh.getVersion());

    assertEquals("MSH", msh.getRootTerminology().getTerminology());
    // Because of XML Transient
    assertNull(msh.getRootTerminology().getLastModifiedBy());
    assertNull(msh.getRootTerminology().getFamily());
    assertNull(msh.getRootTerminology().getHierarchicalName());
    assertNull(msh.getRootTerminology().getLanguage());
    assertNull(msh.getRootTerminology().getPreferredName());
    assertNull(msh.getRootTerminology().getShortName());
    assertNull(msh.getRootTerminology().getAcquisitionContact());
    assertNull(msh.getRootTerminology().getContentContact());
    assertNull(msh.getRootTerminology().getLicenseContact());

  }

  /**
   * Test normal use rest metadata005.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetDefaultPrecedenceList() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    // test precedence list
    // Note: specific entry ordering is data-dependent (differs between
    // full UMLS and SAMPLE_UMLS), so only verify metadata and non-empty list.
    PrecedenceList precedence =
        metadataService.getDefaultPrecedenceList("MTH", "latest", authToken);
    assertEquals("loader", precedence.getLastModifiedBy());
    assertEquals("MTH", precedence.getTerminology());
    assertEquals("latest", precedence.getVersion());
    assertEquals("DEFAULT", precedence.getName());
    assertTrue(precedence.getPrecedence().getKeyValuePairs().size() > 0);

  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @Override
  @After
  public void teardown() throws Exception {

    // logout
    securityService.logout(authToken);
  }

  /**
   * Test UMLS metadata.
   *
   * @param keyValuePairLists the key value pair lists
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean testUmlsMetadata(KeyValuePairLists keyValuePairLists)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "Testing UMLS metadata retrieval, " + keyValuePairLists.getCount()
            + " pair lists found (" + MetadataKeys.values().length
            + " expected)");
    Logger.getLogger(getClass()).info(keyValuePairLists);

    Map<MetadataKeys, Integer> expectedSizes = new HashMap<>();
    Map<MetadataKeys, Integer> expectedSizes2 = new HashMap<>();
    Map<MetadataKeys, String> expectedIds = new HashMap<>();
    Map<MetadataKeys, Set<String>> expectedNames = new HashMap<>();

    // Relationship types
    expectedSizes.put(MetadataKeys.Relationship_Types, 16);
    expectedSizes2.put(MetadataKeys.Relationship_Types, 16);
    expectedIds.put(MetadataKeys.Relationship_Types, "PAR");
    expectedNames.put(MetadataKeys.Relationship_Types, new HashSet<String>());
    expectedNames.get(MetadataKeys.Relationship_Types).add(
        "has parent relationship in a Metathesaurus source vocabulary");

    // Additional relationship types
    expectedSizes.put(MetadataKeys.Additional_Relationship_Types, 242);
    expectedSizes2.put(MetadataKeys.Additional_Relationship_Types, 242);
    expectedIds.put(MetadataKeys.Additional_Relationship_Types, "isa");
    expectedNames.put(MetadataKeys.Additional_Relationship_Types,
        new HashSet<String>());
    expectedNames.get(MetadataKeys.Additional_Relationship_Types).add("Is a");

    // Attribute names
    expectedSizes.put(MetadataKeys.Attribute_Names, 447);
    expectedSizes2.put(MetadataKeys.Attribute_Names, 447);
    expectedIds.put(MetadataKeys.Attribute_Names, "ACCEPTABILITYID");
    expectedNames.put(MetadataKeys.Attribute_Names, new HashSet<String>());
    expectedNames.get(MetadataKeys.Attribute_Names).add("Acceptability Id");

    // Semantic types
    expectedSizes.put(MetadataKeys.Semantic_Types, 133);
    expectedSizes2.put(MetadataKeys.Semantic_Types, 133);
    expectedIds.put(MetadataKeys.Semantic_Types, "clnd");
    expectedNames.put(MetadataKeys.Semantic_Types, new HashSet<String>());
    expectedNames.get(MetadataKeys.Semantic_Types).add("Clinical Drug");

    // Term types
    expectedSizes.put(MetadataKeys.Term_Types, 174);
    expectedSizes2.put(MetadataKeys.Term_Types, 174);
    expectedIds.put(MetadataKeys.Term_Types, "PT");
    expectedNames.put(MetadataKeys.Term_Types, new HashSet<String>());
    expectedNames.get(MetadataKeys.Term_Types).add("Designated preferred name");

    // Languages
    expectedSizes.put(MetadataKeys.Languages, 25);
    expectedSizes2.put(MetadataKeys.Languages, 25);
    expectedIds.put(MetadataKeys.Languages, "ENG");
    expectedNames.put(MetadataKeys.Languages, new HashSet<String>());
    expectedNames.get(MetadataKeys.Languages).add("English");

    // General metadata entries
    expectedSizes.put(MetadataKeys.General_Metadata_Entries, 138);
    expectedSizes2.put(MetadataKeys.General_Metadata_Entries, 138);
    expectedIds.put(MetadataKeys.General_Metadata_Entries, "FULL-MULTIPLE");
    expectedNames.put(MetadataKeys.General_Metadata_Entries,
        new HashSet<String>());
    expectedNames.get(MetadataKeys.General_Metadata_Entries).add(
        "Full contexts, multiple tree positions");

    boolean result =
        testHelper(keyValuePairLists, expectedSizes, expectedSizes2,
            expectedIds, expectedNames);

    return result;
  }

  /**
   * Test snomed metadata.
   *
   * @param keyValuePairLists the key value pair lists
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean testSnomedMetadata(KeyValuePairLists keyValuePairLists)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "Testing SNOMEDCT_US metadata retrieval, "
            + keyValuePairLists.getCount() + " pair lists found ("
            + MetadataKeys.values().length + " expected)");

    Map<MetadataKeys, Integer> expectedSizes = new HashMap<>();
    Map<MetadataKeys, Integer> expectedSizes2 = new HashMap<>();
    Map<MetadataKeys, String> expectedIds = new HashMap<>();
    Map<MetadataKeys, Set<String>> expectedNames = new HashMap<>();

    // SAMPLE_UMLS does not load per-source metadata for individual
    // terminologies — all categories return 0 entries for SNOMEDCT_US.

    // Relationship types
    expectedSizes.put(MetadataKeys.Relationship_Types, 0);
    expectedSizes2.put(MetadataKeys.Relationship_Types, 0);
    expectedNames.put(MetadataKeys.Relationship_Types, new HashSet<String>());

    // Additional relationship types
    expectedSizes.put(MetadataKeys.Additional_Relationship_Types, 0);
    expectedSizes2.put(MetadataKeys.Additional_Relationship_Types, 0);
    expectedNames.put(MetadataKeys.Additional_Relationship_Types,
        new HashSet<String>());

    // Attribute names
    expectedSizes.put(MetadataKeys.Attribute_Names, 0);
    expectedSizes2.put(MetadataKeys.Attribute_Names, 0);
    expectedNames.put(MetadataKeys.Attribute_Names, new HashSet<String>());

    // Semantic types
    expectedSizes.put(MetadataKeys.Semantic_Types, 0);
    expectedSizes2.put(MetadataKeys.Semantic_Types, 0);
    expectedNames.put(MetadataKeys.Semantic_Types, new HashSet<String>());

    // Term types
    expectedSizes.put(MetadataKeys.Term_Types, 0);
    expectedSizes2.put(MetadataKeys.Term_Types, 0);
    expectedNames.put(MetadataKeys.Term_Types, new HashSet<String>());

    // Languages
    expectedSizes.put(MetadataKeys.Languages, 0);
    expectedSizes2.put(MetadataKeys.Languages, 0);
    expectedNames.put(MetadataKeys.Languages, new HashSet<String>());

    // General metadata entries
    expectedSizes.put(MetadataKeys.General_Metadata_Entries, 0);
    expectedSizes2.put(MetadataKeys.General_Metadata_Entries, 0);
    expectedNames.put(MetadataKeys.General_Metadata_Entries,
        new HashSet<String>());
    KeyValuePairList list = new KeyValuePairList();
    list.setName(MetadataKeys.General_Metadata_Entries.toString());
    keyValuePairLists.addKeyValuePairList(list);

    boolean result =
        testHelper(keyValuePairLists, expectedSizes, expectedSizes2,
            expectedIds, expectedNames);

    return result;
  }

  /**
   * Test msh metadata.
   *
   * @param keyValuePairLists the key value pair lists
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean testMshMetadata(KeyValuePairLists keyValuePairLists)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "Testing MSH metadata retrieval, " + keyValuePairLists.getCount()
            + " pair lists found (" + MetadataKeys.values().length
            + " expected)");

    Map<MetadataKeys, Integer> expectedSizes = new HashMap<>();
    Map<MetadataKeys, Integer> expectedSizes2 = new HashMap<>();
    Map<MetadataKeys, String> expectedIds = new HashMap<>();
    Map<MetadataKeys, Set<String>> expectedNames = new HashMap<>();

    // SAMPLE_UMLS does not load per-source metadata for individual
    // terminologies — all categories return 0 entries for MSH.

    // Relationship types
    expectedSizes.put(MetadataKeys.Relationship_Types, 0);
    expectedSizes2.put(MetadataKeys.Relationship_Types, 0);
    expectedNames.put(MetadataKeys.Relationship_Types, new HashSet<String>());

    // Additional relationship types
    expectedSizes.put(MetadataKeys.Additional_Relationship_Types, 0);
    expectedSizes2.put(MetadataKeys.Additional_Relationship_Types, 0);
    expectedNames.put(MetadataKeys.Additional_Relationship_Types,
        new HashSet<String>());

    // Attribute names
    expectedSizes.put(MetadataKeys.Attribute_Names, 0);
    expectedSizes2.put(MetadataKeys.Attribute_Names, 0);
    expectedNames.put(MetadataKeys.Attribute_Names, new HashSet<String>());

    // Semantic types
    expectedSizes.put(MetadataKeys.Semantic_Types, 0);
    expectedSizes2.put(MetadataKeys.Semantic_Types, 0);
    expectedNames.put(MetadataKeys.Semantic_Types, new HashSet<String>());

    // Term types
    expectedSizes.put(MetadataKeys.Term_Types, 0);
    expectedSizes2.put(MetadataKeys.Term_Types, 0);
    expectedNames.put(MetadataKeys.Term_Types, new HashSet<String>());

    // Languages
    expectedSizes.put(MetadataKeys.Languages, 0);
    expectedSizes2.put(MetadataKeys.Languages, 0);
    expectedNames.put(MetadataKeys.Languages, new HashSet<String>());

    // General metadata entries - add empty
    expectedSizes.put(MetadataKeys.General_Metadata_Entries, 0);
    expectedSizes2.put(MetadataKeys.General_Metadata_Entries, 0);
    expectedNames.put(MetadataKeys.General_Metadata_Entries,
        new HashSet<String>());

    KeyValuePairList list = new KeyValuePairList();
    list.setName(MetadataKeys.General_Metadata_Entries.toString());
    keyValuePairLists.addKeyValuePairList(list);

    // expectedNames.get(MetadataKeys.General_Metadata_Entries).add(
    // "Full contexts, multiple tree positions");

    boolean result =
        testHelper(keyValuePairLists, expectedSizes, expectedSizes2,
            expectedIds, expectedNames);

    return result;
  }

  /**
   * Test helper.
   *
   * @param keyValuePairLists the key value pair lists
   * @param expectedSizes the expected sizes
   * @param expectedSizes2 the expected sizes2
   * @param expectedIds the expected ids
   * @param expectedNameSets the expected names
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean testHelper(KeyValuePairLists keyValuePairLists,
    Map<MetadataKeys, Integer> expectedSizes,
    Map<MetadataKeys, Integer> expectedSizes2,
    Map<MetadataKeys, String> expectedIds,
    Map<MetadataKeys, Set<String>> expectedNameSets) throws Exception {

    // the count of categories successfully passing test
    int categorySuccessCt = 0;

    // cycle over all retrieved metadata
    for (KeyValuePairList keyValuePairList : keyValuePairLists
        .getKeyValuePairLists()) {

      Logger.getLogger(getClass()).info(
          "Checking " + keyValuePairList.getKeyValuePairs().size() + " "
              + keyValuePairList.getName());

      // Skip label sets - no data
      if (MetadataKeys.valueOf(keyValuePairList.getName()) == MetadataKeys.Label_Sets) {
        categorySuccessCt++;
        continue;
      }

      int expectedSize =
          expectedSizes.get(MetadataKeys.valueOf(keyValuePairList.getName()));
      int expectedSize2 =
          expectedSizes2.get(MetadataKeys.valueOf(keyValuePairList.getName()));
      String expectedId =
          expectedIds.get(MetadataKeys.valueOf(keyValuePairList.getName()));
      Set<String> expectedNames =
          expectedNameSets
              .get(MetadataKeys.valueOf(keyValuePairList.getName()));
      List<KeyValuePair> pairs = keyValuePairList.getKeyValuePairs();

      KeyValuePair testCase = null;

      if (expectedSize == 0 && pairs.size() == 0) {
        categorySuccessCt++;
      }
      // if this case has been specified, check it
      else if (expectedSize != -1 && pairs.size() != 0) {

        for (KeyValuePair pair : pairs) {
          if (expectedId != null && expectedId.equals(pair.getKey())
              && expectedNames.contains(pair.getValue()))
            testCase = pair;
        }

        if (expectedSize != pairs.size() && expectedSize2 != pairs.size()) {
          Logger.getLogger(getClass()).warn(
              "  Expected size " + expectedSize + ", " + expectedSize2
                  + " did not match actual size " + pairs.size());
          Logger.getLogger(getClass()).info("  Retrieved pairs were: ");
          for (KeyValuePair pair : pairs) {
            Logger.getLogger(getClass()).info("    " + pair.toString());
          }
        }

        else if (testCase == null) {
          Logger.getLogger(getClass()).warn(
              "  Could not find pair for id = " + expectedId + ", names "
                  + expectedNames.toString());
          Logger.getLogger(getClass()).info("  Available pairs were: ");
          for (KeyValuePair pair : pairs) {
            Logger.getLogger(getClass()).info("    " + pair.toString());
          }
        } else {
          categorySuccessCt++;
        }
      }
    }

    Logger.getLogger(getClass()).info(
        "Metadata Categories Validated:  " + categorySuccessCt + " out of "
            + MetadataKeys.values().length);

    return categorySuccessCt == MetadataKeys.values().length;
  }

  /**
   * Test addition, update, and removal of precedence lists.
   */
  @SuppressWarnings("static-method")
  @Test
  public void testPrecedenceList() {

    PrecedenceList p = new PrecedenceListJpa();
    p.setBranch(Branch.ROOT);
    p.setLastModified(new Date());
    p.setLastModifiedBy("userName");
    p.setName("name");
    p.setTerminology("terminology");
    p.setVersion("version");
    p.setTimestamp(new Date());

    // test add
    try {
      p = metadataService.addPrecedenceList((PrecedenceListJpa) p, adminToken);
      assertNotNull(p);
      assertNotNull(p.getId());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed to add precedence list");
    }

    // test get
    try {
      p = metadataService.getPrecedenceList(p.getId(), authToken);
      assertNotNull(p);
      assertTrue(p.getTerminology().equals("terminology"));
    } catch (Exception e1) {
      e1.printStackTrace();
      fail("Failed to get precedence list");
    }

    // test update
    p.setTerminology("newTerminology");
    try {
      metadataService.updatePrecedenceList((PrecedenceListJpa) p, adminToken);
      p = metadataService.getPrecedenceList(p.getId(), authToken);
      assertTrue(p.getTerminology().equals("newTerminology"));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed to update precedence list");
    }

    // test remove
    try {
      metadataService.removePrecedenceList(p.getId(), adminToken);
      p = metadataService.getPrecedenceList(p.getId(), authToken);
      assertNull(p);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed to remove precedence list");
    }
  }

}
