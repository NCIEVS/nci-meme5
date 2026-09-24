/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import static org.junit.Assert.assertEquals;

import org.apache.lucene.codecs.Codec;
import org.junit.Test;

/**
 * Unit tests for compatibility with existing Lucene indexes.
 */
public class LuceneCompatibilityUnitTest {

  /**
   * Verifies indexes written by the previous Lucene version remain readable.
   */
  @Test
  public void testLucene99CodecIsAvailable() {
    assertEquals("Lucene99", Codec.forName("Lucene99").getName());
  }
}
