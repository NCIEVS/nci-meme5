/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.wci.umls.server.helpers.KeyValuePair;
import com.wci.umls.server.helpers.KeyValuePairList;
import com.wci.umls.server.jpa.model.content.ConceptRelationshipJpa;
import com.wci.umls.server.jpa.model.helpers.PrecedenceListJpa;

/**
 * Unit testing for {@link RrfComputePreferredNameHandler}.
 */
public class RrfComputePreferredNameHandlerUnitTest {

  /**
   * Test that concurrent first use waits for both rank maps to be cached before
   * treating a precedence list as cached.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConcurrentCacheListWaitsForFullyCachedList()
    throws Exception {

    final RrfComputePreferredNameHandler handler =
        new RrfComputePreferredNameHandler();
    handler.clearCaches();
    final BlockingPrecedenceList list = new BlockingPrecedenceList();
    final List<ConceptRelationshipJpa> relationships = Arrays.asList(
        getRelationship(1L), getRelationship(2L));
    final ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      final Future<List<ConceptRelationshipJpa>> first =
          executor.submit(() -> handler.sortRelationships(relationships, list));
      assertTrue(list.awaitTermTypeRankMapCall());

      final Future<List<ConceptRelationshipJpa>> second =
          executor.submit(() -> handler.sortRelationships(relationships, list));
      list.releaseTermTypeRankMap();

      assertEquals(2, first.get(5, TimeUnit.SECONDS).size());
      assertEquals(2, second.get(5, TimeUnit.SECONDS).size());
    } finally {
      list.releaseTermTypeRankMap();
      executor.shutdownNow();
      handler.clearCaches();
    }
  }

  /**
   * Returns the relationship.
   *
   * @param id the id
   * @return the relationship
   */
  private static ConceptRelationshipJpa getRelationship(final Long id) {
    final ConceptRelationshipJpa relationship = new ConceptRelationshipJpa();
    relationship.setId(id);
    relationship.setTerminologyId("R" + id);
    relationship.setTerminology("NCI");
    relationship.setVersion("v1");
    relationship.setPublishable(true);
    relationship.setObsolete(false);
    relationship.setSuppressible(false);
    return relationship;
  }

  /**
   * A precedence list that blocks while building the first term type rank map.
   */
  private static class BlockingPrecedenceList extends PrecedenceListJpa {

    /** The term type rank map called latch. */
    private final CountDownLatch termTypeRankMapCalled =
        new CountDownLatch(1);

    /** The release term type rank map latch. */
    private final CountDownLatch releaseTermTypeRankMap =
        new CountDownLatch(1);

    /** Indicates whether the first call has been blocked. */
    private final AtomicBoolean blocked = new AtomicBoolean(false);

    /**
     * Instantiates a {@link BlockingPrecedenceList}.
     */
    BlockingPrecedenceList() {
      setId(1L);
      setLastModified(new Date(1000L));
      setLastModifiedBy("tester");
      setTerminology("NCI");
      setVersion("v1");
      setName("test");

      final KeyValuePairList precedence = new KeyValuePairList();
      precedence.addKeyValuePair(new KeyValuePair("NCI", "PT"));
      setPrecedence(precedence);
    }

    /* see superclass */
    @Override
    public Map<String, String> getTermTypeRankMap() {
      if (blocked.compareAndSet(false, true)) {
        termTypeRankMapCalled.countDown();
        try {
          assertTrue(releaseTermTypeRankMap.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException(e);
        }
      }
      return super.getTermTypeRankMap();
    }

    /* see superclass */
    @Override
    public int hashCode() {
      return super.hashCode();
    }

    /* see superclass */
    @Override
    public boolean equals(final Object obj) {
      return super.equals(obj);
    }

    /**
     * Await the term type rank map call.
     *
     * @return true, if the call was reached
     * @throws InterruptedException the interrupted exception
     */
    private boolean awaitTermTypeRankMapCall() throws InterruptedException {
      return termTypeRankMapCalled.await(5, TimeUnit.SECONDS);
    }

    /**
     * Release the term type rank map call.
     */
    private void releaseTermTypeRankMap() {
      releaseTermTypeRankMap.countDown();
    }
  }
}
