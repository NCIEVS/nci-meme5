/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Query;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.helpers.SearchResultList;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;

/**
 * In effort to reduce deleted_cuis, create bequeathals to the live parent 
 * or grandparent concept.
 */
public class CreateEfficientAncestorBequeathalAlgorithm extends AbstractInsertMaintReleaseAlgorithm {

  /**
   * Instantiates an empty {@link CreateEfficientAncestorBequeathalAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public CreateEfficientAncestorBequeathalAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("CREATEEFFICIENTANCESTORBEQUEATH");
    setLastModifiedBy("admin");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    ValidationResult validationResult = new ValidationResultJpa();

    if (getProject() == null) {
      throw new Exception("Create ancestor bequeath requires a project to be set");
    }

    // Check the input directories

    final String srcFullPath =
        ConfigUtility.getConfigProperties().getProperty("source.data.dir")
            + File.separator + getProcess().getInputPath();

    setSrcDirFile(new File(srcFullPath));
    /**if (!getSrcDirFile().exists()) {
      throw new Exception("Specified input directory does not exist");
    } */

    return validationResult;
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    int addedCount = 0;
    
    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    try {

      Set<Concept> deletedCuis = new HashSet<>();
      File srcDir = getSrcDirFile();
      File maintDir = new File(srcDir, "maint");
      if (! maintDir.exists()){
        maintDir.mkdir();
      }
      logInfo("maint dir:" + maintDir);
      BufferedWriter out = new BufferedWriter(new FileWriter(new File(maintDir, "bequeathal.ancestor.relationships.src")));
      
      Query query = getEntityManager().createNativeQuery(
          "SELECT   DISTINCT c.id conceptId FROM   concepts c,   "
          + "concepts_atoms ca,   atoms a WHERE   c.terminology = 'NCIMTH'   "
          + "AND c.id != c.terminologyId   AND c.id = ca.concepts_id   AND "
          + "ca.atoms_id = a.id   "
          + "AND a.publishable = FALSE   AND NOT c.id IN ("
          +   "SELECT       DISTINCT c.id conceptId     " 
          +   " FROM       concepts c,       concepts_atoms ca,       atoms a     "
          +   " WHERE       c.terminology = 'NCIMTH'       AND c.id = ca.concepts_id   "
          +   " AND ca.atoms_id = a.id       AND a.publishable = TRUE   )   " 
          +   " AND NOT c.id IN (     "
          +   "   SELECT       DISTINCT c.id conceptId     "
          +   "    FROM       concepts c,       concept_relationships cr     "
          +   "    WHERE       c.terminology = 'NCIMTH'       AND c.id = cr.from_id       "
          +   "    AND cr.relationshipType like 'B%'   )   AND NOT c.id IN (     "
          +   "      SELECT       c.id conceptId     " 
          +   "        FROM       concepts c,       concepts_atoms ca     "
          +   "        WHERE       c.terminology = 'NCIMTH'       "
          +   "        AND c.id = ca.concepts_id       AND ca.concepts_id IN (         "
          +   "      SELECT           ca.concepts_id         FROM           concepts_atoms ca,           atoms a         " 
          +   "        WHERE           ca.atoms_id = a.id           "
          +   "        AND a.terminology IN (:terminology, 'NCIMTH')           " 
          +   "        AND a.termType = 'PN'       )     GROUP BY       ca.concepts_id     "
          +   "        HAVING       COUNT(DISTINCT ca.atoms_id) = 1   )"
          +   " AND NOT c.id IN (   "
          +   "   SELECT  " 
          +   "     ca.concepts_id conceptId  "
          +   "   FROM  "
          +   "     mrcui mr,  "
          +  "      atomjpa_conceptterminologyids ac,  "
          +  "      concepts_atoms ca,  "
          +  "      concepts cpt  "
          + "     WHERE  "
          +  "      mr.cui1 = ac.conceptTerminologyIds  "
          +  "      AND ca.atoms_id = ac.AtomJpa_id  "
          +  "      AND cpt.id = ca.concepts_id  "
          +  "      AND cpt.terminology = 'NCIMTH'  "
          +  "      AND ac.conceptTerminologyIds_KEY = 'NCIMTH'  "
          +  "      AND mr.rel = 'DEL'  )"
        
          
          );
      
      query.setParameter("terminology",
              getProcess().getTerminology());
      List<Object> list = query.getResultList();
      setSteps(list.size());
      //List<Object> list = new ArrayList<>();
      //list.add("25250919");
      //list.add("30310940");
      //list.add("30311007");
      //list.add("30311025");
      
      int index = 1;
      for (final Object entry : list) {
        final Long id = Long.valueOf(entry.toString());
        Concept c = getConcept(id);
        deletedCuis.add(c);
        c.getAtoms().size();
        c.getRelationships().size();
      } 
      
      // compute atom rels used in deletedCuis, NCBI first
      List <RelObject> rels = new ArrayList<>();
      Map<String,  List<RelObject>> relMap = new HashMap<>();
      Query relQuery = getEntityManager().createNativeQuery(
          "select c1.terminologyId cui1, c2.terminologyId cui2, c2.publishable " +
          "FROM atom_relationships r, concepts_atoms ca1, concepts_atoms ca2, concepts c1, concepts c2 " +
           "WHERE   r.terminology = 'NCBI'  AND r.to_id = ca1.atoms_id  AND r.from_id = ca2.atoms_id " +
          " AND c1.id = ca1.concepts_id  AND c2.id = ca2.concepts_id " + 
          " AND c1.terminology='NCIMTH' AND c2.terminology='NCIMTH' AND r.relationshipType = 'PAR'"); 
      //relQuery.setParameter("terminology",getProcess().getTerminology());
      //Query relQuery = getEntityManager().createNativeQuery(
      //    "select cui1, cui2, publishable from relMap");
      List<Object[]> results = relQuery.getResultList();
      for (final Object[] entry : results) {
        final String fromCui = entry[0].toString();
        final String toCui = entry[1].toString();
        final Boolean toPublishable = Boolean.parseBoolean(entry[2].toString());
        RelObject rel = new RelObject(fromCui, toCui, toPublishable);
        rels.add(rel);
      }
      System.out.println("rels.size: " + rels.size());
      
      for (RelObject rel : rels) {
        if (relMap.containsKey(rel.getCui1())) {
          List<RelObject> llist = relMap.get(rel.getCui1());
          llist.add(rel);
          relMap.put(rel.getCui1(), llist);
        } else {
          List<RelObject> llist = new ArrayList<>();
          llist.add(rel);
          relMap.put(rel.getCui1(), llist);
        }
      }
      System.out.println("relMap.size: " + relMap.size());
      Entry<String, List<RelObject>> entry = relMap.entrySet().iterator().next();
      System.out.println("relMap entry: " + entry.getKey() + " ** " + entry.getValue().get(0));
      
      for (Concept cpt : deletedCuis) {
        Set<String> pathResults = computeTransitiveClosure(cpt.getTerminologyId(),relMap,"");
        
        if (pathResults != null) {
          for (String result : pathResults) {

            // System.out.println("path: " + result);
            // this returns things like .CUI1.CUI2.CUI3.CUI4 => bequeathal rel
            // is
            // CUI1=>CUI4
            String[] tokens = FieldedStringTokenizer.split(result, ".");
            StringBuffer sb = new StringBuffer();
            sb.append("").append("|");
            sb.append("C").append("|");
            sb.append(tokens[1]).append("|");
            sb.append("BBT").append("|").append("|");
            sb.append(tokens[tokens.length - 1]).append("|");
            sb.append("NCIMTH|NCIMTH|R|n|N|N|SOURCE_CUI|NCIMTH|SOURCE_CUI|NCIMTH|||").append("\n");
            System.out.print(sb.toString());
          }
        }
        
        
        System.out.println("\n");
        // decide what to do if the cui2s set had > 5 entries.
        // write bequeathal rel entries 
      }
 

      commitClearBegin();

      logInfo("  added count = " + addedCount);

      logInfo("Finished " + getName());

    } catch (

    Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      throw e;
    }

  }

  public Set<String> computeTransitiveClosure(String cui, Map<String, List<RelObject>> relMap, String ancestorPath) {
    // assumption: cui is not publishable  

    // check for cycles
    if (ancestorPath.contains(cui+".")) {
      return new HashSet<>(0);
    }

    Set<String> results = new HashSet<>();
    List<RelObject> relObjs = relMap.get(cui);
    if (relObjs == null) {
      return null;
    }
    for (RelObject rel : relMap.get(cui)) {
      // String cui1 = rel[0].toString();
      String cui2 = rel.getCui2();
      boolean publishable = Boolean.valueOf(rel.isCui2Publishable());
      if (publishable) {
         results.add ((ancestorPath.equals("") ? "" :  (ancestorPath + ".")) + cui + "." + cui2);
      }
      else {
         results.addAll(computeTransitiveClosure(cui2, relMap, ancestorPath + "." + cui));
      }
    }

    return results;
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

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();

    return params;
  }

  @Override
  public String getDescription() {
    return "Bequeaths deleted cuis to their closest published ancestor";
  }

  private class RelObject {
    private String cui1;
    private String cui2;
    private boolean cui2Publishable;
    
    public RelObject(String cui1, String cui2, Boolean publishable) {
      setCui1(cui1);
      setCui2(cui2);
      setCui2Publishable(publishable);
    }
    
    public String getCui1() {
      return cui1;
    }
    public void setCui1(String cui1) {
      this.cui1 = cui1;
    }
    public String getCui2() {
      return cui2;
    }
    public void setCui2(String cui2) {
      this.cui2 = cui2;
    }
    public boolean isCui2Publishable() {
      return cui2Publishable;
    }
    public void setCui2Publishable(boolean cui2Publishable) {
      this.cui2Publishable = cui2Publishable;
    }
    
    
  }
}