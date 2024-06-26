package com.wci.umls.server.jpa.services.helper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wci.umls.server.helpers.PrecedenceList;
import com.wci.umls.server.jpa.services.handlers.RrfComputePreferredNameHandler;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Concept;

/**
 * LexicalClass/StringClass comparator for report.
 */
public class ReportsAtomComparator implements Comparator<Atom> {

  /** The lui ranks. */
  private Map<String, String> luiRanks = new HashMap<>();

  /** The sui ranks. */
  private Map<String, String> suiRanks = new HashMap<>();

  /** The atom ranks. */
  private Map<Long, String> atomRanks = new HashMap<>();

  /**
   * Instantiates a {@link ReportsAtomComparator} from the specified parameters.
   *
   * @param concept the concept
   * @param list the list
   * @throws Exception the exception
   */
  public ReportsAtomComparator(final Concept concept, final PrecedenceList list)
      throws Exception {

    // Set up vars

    // Configure rank handler
    final RrfComputePreferredNameHandler handler =
        new RrfComputePreferredNameHandler();
    handler.cacheList(list);

    // Get default atom ordering
    final List<Atom> atoms = concept.getAtoms();

    // Iterate through atoms, maintaning the luiRanks and suiRanks maps
    for (final Atom atom : atoms) {

      // Get initial values
      final String lui = atom.getLexicalClassId();
      final String sui = atom.getStringClassId();
      final String rank = handler.getRank(atom, list);
      atomRanks.put(atom.getId(), rank);

      // Look up that atom's lui
      final String luiRank = luiRanks.get(lui);
      if (luiRank == null) {
        // Add the current atom's lui and rank to the hashmap.
        luiRanks.put(lui, rank);
      }

      // Compare the rank returned with the current rank
      // and determine which rank is higher.
      else if (rank.compareTo(luiRank) > 0) {
        // if the current atom's rank is higher than the one in the hashmap,
        // then replace it.
        luiRanks.put(lui, rank);

        // Look up that atom's sui in suiRanks
      }
      final String suiRank = suiRanks.get(sui);
      if (suiRank == null) {
        // Add the current atom's sui and rank to the hashmap.
        suiRanks.put(sui, rank);
      }

      // Compare the rank returned with the current rank
      // and determine which rank is higher.
      else if (rank.compareTo(suiRank) > 0) {
        // if the current atom's rank is higher than the one in the hashmap,
        // then replace it.
        suiRanks.put(sui, rank);
      }

    } // end for
  }

  /* see superclass */
  @Override
  public int compare(final Atom a1, final Atom a2) {

    // Reverse sort -> return the higher value first

    // Compare languages
    if (!a1.getLanguage().equals(a2.getLanguage())) {
      if (a1.getLanguage().equals("ENG") && !a2.getLanguage().equals("ENG")) {
        return -1;
      }
      if (a2.getLanguage().equals("ENG") && !a1.getLanguage().equals("ENG")) {
        return 1;
      }
      // alphabetical languages after ENG
      return a1.getLanguage().compareTo(a2.getLanguage());
    }
    // Compare LUI ranks first
    if (!a1.getLexicalClassId().equals(a2.getLexicalClassId())) {
      return luiRanks.get(a2.getLexicalClassId())
          .compareTo(luiRanks.get(a1.getLexicalClassId()));
    }

    // Compare SUI ranks second
    if (!a1.getStringClassId().equals(a2.getStringClassId())) {
      return suiRanks.get(a2.getStringClassId())
          .compareTo(suiRanks.get(a1.getStringClassId()));
    }

    // If things are STILL equal, compare the ranks
    return atomRanks.get(a2.getId()).compareTo(atomRanks.get(a1.getId()));
  }
}
