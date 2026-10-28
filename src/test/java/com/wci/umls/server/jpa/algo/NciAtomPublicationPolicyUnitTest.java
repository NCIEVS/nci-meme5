package com.wci.umls.server.jpa.algo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.wci.umls.server.jpa.model.content.AtomJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/** Database-free regression checks for the established NCI release policy. */
public class NciAtomPublicationPolicyUnitTest {

  @Test
  public void suppressesIncomingCvxAndVandfAbbreviationsWithoutObsoletingThem() {
    for (String source : new String[] {"CVX", "VANDF"}) {
      final Atom atom = atom(source, "AB", false, false);

      assertTrue(NciAtomPublicationPolicy.apply("NCIMTH", atom));
      assertTrue(atom.isSuppressible());
      assertFalse(atom.isObsolete());
      assertFalse(NciAtomPublicationPolicy.apply("NCIMTH", atom));
    }
  }

  @Test
  public void preservesAbbreviationObsolescenceForEveryIncomingFlagCombination() {
    for (boolean obsolete : new boolean[] {false, true}) {
      for (boolean suppressible : new boolean[] {false, true}) {
        final Atom atom = atom("VANDF", "AB", suppressible, obsolete);

        assertEquals(!suppressible, NciAtomPublicationPolicy.apply("NCIMTH", atom));
        assertTrue(atom.isSuppressible());
        assertEquals(obsolete, atom.isObsolete());
        assertFalse(NciAtomPublicationPolicy.apply("NCIMTH", atom));
      }
    }
  }

  @Test
  public void suppressesAndObsoletesOpAndIsForEveryIncomingFlagCombination() {
    for (String termType : new String[] {"OP", "IS"}) {
      for (boolean obsolete : new boolean[] {false, true}) {
        for (boolean suppressible : new boolean[] {false, true}) {
          final Atom atom = atom("NCI", termType, suppressible, obsolete);

          assertEquals(!suppressible || !obsolete,
              NciAtomPublicationPolicy.apply("NCIMTH", atom));
          assertTrue(atom.isSuppressible());
          assertTrue(atom.isObsolete());
          assertFalse(NciAtomPublicationPolicy.apply("NCIMTH", atom));
        }
      }
    }
  }

  @Test
  public void preservesOtherTermTypesIncludingTheirIncomingSuppression() {
    for (String termType : new String[] {"PT", "SY", "OA", "MTH_OP", "VAB"}) {
      for (boolean obsolete : new boolean[] {false, true}) {
        for (boolean suppressible : new boolean[] {false, true}) {
          final Atom atom = atom("CVX", termType, suppressible, obsolete);

          assertFalse(NciAtomPublicationPolicy.apply("NCIMTH", atom));
          assertEquals(suppressible, atom.isSuppressible());
          assertEquals(obsolete, atom.isObsolete());
        }
      }
    }
  }

  @Test
  public void preservesSourceFlagsInOtherProjectsEvenForNcimthOwnedAtoms() {
    for (String project : new String[] {"MTH", "NCI", "SNOMEDCT_US", "", null}) {
      for (String termType : new String[] {"AB", "OP", "IS"}) {
        final Atom atom = atom("NCIMTH", termType, false, false);

        assertFalse(NciAtomPublicationPolicy.apply(project, atom));
        assertFalse(atom.isSuppressible());
        assertFalse(atom.isObsolete());
      }
    }
  }

  @Test
  public void preservesIdentityWorkflowAndPublicationFlags() {
    for (boolean publishable : new boolean[] {false, true}) {
      final Atom atom = atom("CVX", "AB", false, false);
      atom.setId(8192936L);
      atom.setName("BCG");
      atom.setVersion("2026_02_12");
      atom.setCodeId("19");
      atom.setPublishable(publishable);
      atom.setPublished(true);
      atom.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

      assertTrue(NciAtomPublicationPolicy.apply("NCIMTH", atom));
      assertEquals(Long.valueOf(8192936L), atom.getId());
      assertEquals("CVX", atom.getTerminology());
      assertEquals("AB", atom.getTermType());
      assertEquals("BCG", atom.getName());
      assertEquals("2026_02_12", atom.getVersion());
      assertEquals("19", atom.getCodeId());
      assertEquals(publishable, atom.isPublishable());
      assertTrue(atom.isPublished());
      assertEquals(WorkflowStatus.READY_FOR_PUBLICATION, atom.getWorkflowStatus());
    }
  }

  private static Atom atom(String source, String termType, boolean suppressible,
    boolean obsolete) {
    final Atom atom = new AtomJpa();
    atom.setTerminology(source);
    atom.setTermType(termType);
    atom.setSuppressible(suppressible);
    atom.setObsolete(obsolete);
    return atom;
  }
}
