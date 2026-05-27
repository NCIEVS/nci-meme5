/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.wci.umls.server.helpers.PrecedenceList;
import com.wci.umls.server.jpa.model.AbstractConfigurable;
import com.wci.umls.server.jpa.services.MetadataServiceJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Relationship;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.services.MetadataService;
import com.wci.umls.server.services.handlers.ComputePreferredNameHandler;

/**
 * Implementation {@link ComputePreferredNameHandler} for data with term-type
 * ordering.
 */
public class RrfComputePreferredNameHandler extends AbstractConfigurable
    implements ComputePreferredNameHandler {

  /** The precedenceList lastModifiedDate map. */
  private static final Map<Long, Date> precedenceListLastModifiedMap =
      new HashMap<>();

  /** The tty rank map. */
  private static final Map<Long, Map<String, String>> ttyRankMap =
      new HashMap<>();

  /** The terminology rank map. */
  private static final Map<Long, Map<String, String>> terminologyRankMap =
      new HashMap<>();

  /** The terminology/versions -> current map. */
  private static final Map<String, Boolean> currentTerminologies =
      new HashMap<>();

  /** The cache lock. */
  private static final Object CACHE_LOCK = new Object();

  /** The current terminology cache lock. */
  private static final Object CURRENT_TERMINOLOGY_LOCK = new Object();

  /**
   * Instantiates an empty {@link RrfComputePreferredNameHandler}.
   */
  public RrfComputePreferredNameHandler() {
    // n/a
  }

  /* see superclass */
  @Override
  public String computePreferredName(final Collection<Atom> atoms,
    final PrecedenceList list) throws Exception {

    cacheList(list);
    // Use ranking algorithm from MetamorphoSys
    // [tbr][termgroupRank][lrr][inverse SUI][inverse AUI]
    // LRR isn't available here so just don't worry about it.
    String maxRank = "";
    Atom maxAtom = null;
    for (final Atom atom : atoms) {
      final String rank = getRank(atom, list);
      if (maxAtom == null) {
        maxAtom = atom;
        maxRank = rank;
      } else if (rank.compareTo(maxRank) > 0) {
        maxAtom = atom;
        maxRank = rank;
      }
    }

    if (maxAtom != null) {
      return maxAtom.getName();
    }
    return null;
  }

  /* see superclass */
  @Override
  public List<Atom> sortAtoms(final Collection<Atom> atoms,
    final PrecedenceList list) throws Exception {

    cacheList(list);

    final List<Atom> sortedAtoms = new ArrayList<>(atoms);
    // Get each atom rank
    final Map<Long, String> atomRanks = new HashMap<>();
    for (final Atom atom : atoms) {
      final String rank = getRank(atom, list);
      atomRanks.put(atom.getId(), rank);
    }
    // Sort by atom rank - this works because atom ranks are designed to be
    // fixed-length strings that are directly comparable where higher
    // values are ranked better
    Collections.sort(sortedAtoms, new Comparator<Atom>() {
      @Override
      public int compare(Atom o1, Atom o2) {
        return atomRanks.get(o2.getId()).compareTo(atomRanks.get(o1.getId()));
      }
    });

    return sortedAtoms;
  }

  /**
   * Returns the rank.
   *
   * @param atom the atom
   * @param list the list
   * @return the rank
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  public String getRank(final Atom atom, final PrecedenceList list)
    throws Exception {

    // Bail if no list specified or found
    if (list == null) {
      return "000000000000000000000000000";
    }

    final Map<String, String> ttyRanks = getTermTypeRanks(list);
    final boolean current = isCurrentTerminology(atom);
    // Compute the rank as a fixed length string
    // [publishable][isCurrent][obsolete][suppressible][tty
    // rank][lrr][SUI][atomId]
    // Higher values are better.
    if (!atom.getStringClassId().isEmpty()) {
      return "" + (atom.isPublishable() ? 1 : 0)
          + (current ? 1 : 0)
          + (atom.isObsolete() ? 0 : 1) + (atom.isSuppressible() ? 0 : 1)
          + ttyRanks.get(atom.getTerminology() + "/" + atom.getTermType())
          + atom.getLastPublishedRank()
          + +(10000000000L
              - Long.parseLong(atom.getStringClassId().substring(1)))
          + (100000000000L - atom.getId());
    } else {
      return "" + (atom.isPublishable() ? 1 : 0)
          + (current ? 1 : 0)
          + (atom.isObsolete() ? 0 : 1) + (atom.isSuppressible() ? 0 : 1)
          + ttyRanks.get(atom.getTerminology() + "/" + atom.getTermType())
          + atom.getLastPublishedRank() + (100000000000L - atom.getId());
    }

  }

  /**
   * Returns the rank for the relationship.
   *
   * @param <T> the
   * @param relationship the rel
   * @param list the list
   * @return the rank
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  public <T extends Relationship<?, ?>> String getRank(final T relationship,
    final PrecedenceList list) throws Exception {
    // Bail if no list specified or found
    if (list == null) {
      return "0000000000000000000";
    }

    // Compute the rank as a fixed length string
    // [SAB matching list, e.g. project][publishable][terminology_rank][id]
    // Higher values are better.
    final Map<String, String> terminologyRanks =
        getTerminologyRanks(list);

    // compute rank
    return ""
        + (relationship.getTerminology().equals(list.getTerminology()) ? 1 : 0)
        + (relationship.isPublishable() ? 1 : 0)
        + (relationship.isObsolete() ? 0 : 1)
        + (relationship.isSuppressible() ? 0 : 1)
        + terminologyRanks.get(relationship.getTerminology())
        + (100000000000L - relationship.getId());

  }

  /**
   * Cache list.
   *
   * @param list the list
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  public void cacheList(PrecedenceList list) throws Exception {

    // No list - simply return to try something new
    if (list == null) {
      return;
    }

    synchronized (CACHE_LOCK) {
      // Bail if configured already and if precedence list hasn't changed since
      // it was cached.
      if (precedenceListLastModifiedMap.containsKey(list.getId())
          && ttyRankMap.containsKey(list.getId())
          && terminologyRankMap.containsKey(list.getId())) {
        if (Objects.equals(precedenceListLastModifiedMap.get(list.getId()),
            list.getLastModified())) {
          return;
        }
      }

      // If this list has been updated since it was last cached, clear its
      // values.
      removeListFromCachesHelper(list.getId());

      // Otherwise, build the TTY map.
      final Map<String, String> ttyRanks =
          new HashMap<>(list.getTermTypeRankMap());
      ttyRankMap.put(list.getId(), ttyRanks);

      // Otherwise, build the terminology map.
      final Map<String, String> terminologyRanks =
          new HashMap<>(list.getTerminologyRankMap());
      terminologyRankMap.put(list.getId(), terminologyRanks);

      // Publish the timestamp only after both rank maps are available. The
      // timestamp is the marker that this list is fully cached.
      precedenceListLastModifiedMap.put(list.getId(), list.getLastModified());

    }

  }

  /**
   * Returns the cached term type ranks.
   *
   * @param list the list
   * @return the term type ranks
   * @throws Exception the exception
   */
  private Map<String, String> getTermTypeRanks(final PrecedenceList list)
    throws Exception {

    synchronized (CACHE_LOCK) {
      final Map<String, String> ranks = ttyRankMap.get(list.getId());
      if (ranks != null) {
        return ranks;
      }
    }

    cacheList(list);

    synchronized (CACHE_LOCK) {
      final Map<String, String> ranks = ttyRankMap.get(list.getId());
      if (ranks == null) {
        throw new Exception(
            "Unexpected condition, list is not cached - " + list.getId());
      }
      return ranks;
    }
  }

  /**
   * Returns the cached terminology ranks.
   *
   * @param list the list
   * @return the terminology ranks
   * @throws Exception the exception
   */
  private Map<String, String> getTerminologyRanks(final PrecedenceList list)
    throws Exception {

    synchronized (CACHE_LOCK) {
      final Map<String, String> ranks = terminologyRankMap.get(list.getId());
      if (ranks != null) {
        return ranks;
      }
    }

    cacheList(list);

    synchronized (CACHE_LOCK) {
      final Map<String, String> ranks = terminologyRankMap.get(list.getId());
      if (ranks == null) {
        throw new Exception(
            "Unexpected condition, list is not cached - " + list.getId());
      }
      return ranks;
    }
  }

  /**
   * Indicates whether the atom's terminology/version is current.
   *
   * @param atom the atom
   * @return true, if current
   * @throws Exception the exception
   */
  private boolean isCurrentTerminology(final Atom atom) throws Exception {

    final String key = atom.getTerminology() + atom.getVersion();
    synchronized (CURRENT_TERMINOLOGY_LOCK) {
      final Boolean current = currentTerminologies.get(key);
      if (current != null) {
        return current.booleanValue();
      }
    }

    final MetadataService service = new MetadataServiceJpa();
    try {
      final Terminology terminology =
          service.getTerminology(atom.getTerminology(), atom.getVersion());
      final boolean current = terminology.isCurrent();
      synchronized (CURRENT_TERMINOLOGY_LOCK) {
        currentTerminologies.put(key, current);
      }
      return current;
    } finally {
      service.close();
    }
  }

  /**
   * Remove a single precedence list's content from the caches. The caller must
   * hold {@link #CACHE_LOCK}.
   *
   * @param precedenceListId the list id
   */
  private static void removeListFromCachesHelper(final Long precedenceListId) {
    ttyRankMap.remove(precedenceListId);
    terminologyRankMap.remove(precedenceListId);
    precedenceListLastModifiedMap.remove(precedenceListId);
  }

  /* see superclass */
  @Override
  public void clearCaches() throws Exception {
    synchronized (CACHE_LOCK) {
      ttyRankMap.clear();
      terminologyRankMap.clear();
      precedenceListLastModifiedMap.clear();
    }
    synchronized (CURRENT_TERMINOLOGY_LOCK) {
      currentTerminologies.clear();
    }
  }

  /**
   * Remove a single precedence list's content from the caches.
   *
   * @param precedenceListId the list id
   * @throws Exception the exception
   */
  public void removeListFromCaches(Long precedenceListId) throws Exception {
    synchronized (CACHE_LOCK) {
      removeListFromCachesHelper(precedenceListId);
    }
  }

  /* see superclass */
  @Override
  public String getName() {
    return "RRF Compute Preferred Name Handler";
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public <T extends Relationship<?, ?>> List<T> sortRelationships(
    Collection<T> rels, PrecedenceList list) throws Exception {
    cacheList(list);

    final List<T> sortedRels = new ArrayList<>(rels);
    // Get each rel rank
    final Map<T, String> relRanks = new HashMap<>();
    for (final T rel : rels) {
      final String rank = getRank(rel, list);
      relRanks.put(rel, rank);
    }
    // Sort by rel rank - this works because rel ranks are designed to be
    // fixed-length strings that are directly comparable
    Collections.sort(sortedRels, new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        return relRanks.get(o2).compareTo(relRanks.get(o1));
      }
    });
    return sortedRels;
  }

}
