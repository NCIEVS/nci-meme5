/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.wci.umls.server.jpa.model.content.ConceptRelationshipJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.model.content.Relationship;
import com.wci.umls.server.services.ContentService;

/**
 * Admin tool which performs an ad-hoc administrative operation.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminAdHoc
 * </pre>
 */
public class AdHoc {

  /** Logger. */
  private static final Logger LOG = Logger.getLogger(AdHoc.class.getName());

  /**
   * Main entry point.
   *
   * @param args ignored
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {

    LOG.info("Ad Hoc admin task");

    ContentService service = new ContentServiceJpa();
    service.setMolecularActionFlag(false);
    service.setLastModifiedBy("admin");
    List<Long> relationshipIdList = new ArrayList<>(Arrays.asList(22169L,
        597961L, 619377L, 667915L, 668484L, 671496L, 893580L, 893582L,
        1207522L, 1225496L, 1421696L, 1421966L, 2109914L, 2307574L, 484709L,
        1225691L, 2111078L, 3488547L, 1386493L, 899324L, 62439L, 2357536L,
        674999L, 1834806L, 200L, 3131152L, 316346L, 3385799L, 260423L,
        399152L, 882616L, 1678646L, 1777694L, 1845861L, 2140992L, 603393L,
        2295591L, 598889L, 3131156L, 197610L, 628261L, 619062L, 2084667L,
        222600L, 645235L, 565707L, 1429464L, 424637L, 565100L, 613530L,
        628263L, 2283515L, 619064L, 2251380L, 316752L, 610928L, 612762L,
        630923L, 675001L, 1069151L, 1429466L, 673925L, 1758227L, 2359361L,
        601408L, 601410L, 600221L, 2283517L, 2189993L, 407993L, 15661L,
        295732L, 295785L, 296792L, 469563L, 469663L, 469806L, 676562L,
        1054180L, 1121829L, 1417990L, 1556380L, 1640134L, 2043491L, 424639L,
        424641L, 1071092L, 2167479L, 93249L, 115859L, 1416527L, 2096437L,
        651514L, 484711L, 73079L, 73427L, 75255L, 75270L, 1411537L, 2257798L,
        73025L, 73027L, 571062L, 73030L, 73032L, 78949L, 78889L, 3385801L));

    for (Long relationshipId : relationshipIdList) {
      Relationship<?, ?> rel =
          service.getRelationship(relationshipId, ConceptRelationshipJpa.class);
      if (rel != null) {
        service.removeRelationship(relationshipId, ConceptRelationshipJpa.class);
      }
    }

    LOG.info("done ...");
  }
}
