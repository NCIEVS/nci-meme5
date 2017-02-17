/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.jpa.content.AttributeJpa;
import com.wci.umls.server.jpa.content.DefinitionJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomClass;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.ComponentHasAttributes;
import com.wci.umls.server.model.content.ComponentHasDefinitions;
import com.wci.umls.server.model.content.Definition;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.services.RootService;
import com.wci.umls.server.services.handlers.ComputePreferredNameHandler;
import com.wci.umls.server.services.handlers.IdentifierAssignmentHandler;

/**
 * Implementation of an algorithm to import attributes.
 */
public class AttributeLoaderAlgorithm
    extends AbstractInsertMaintReleaseAlgorithm {

  /**
   * Instantiates an empty {@link AttributeLoaderAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public AttributeLoaderAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("ATTRIBUTELOADER");
    setLastModifiedBy("admin");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    ValidationResult validationResult = new ValidationResultJpa();

    if (getProject() == null) {
      throw new Exception("Attribute Loading requires a project to be set");
    }

    // Check the input directories

    final String srcFullPath =
        ConfigUtility.getConfigProperties().getProperty("source.data.dir")
            + File.separator + getProcess().getInputPath();

    setSrcDirFile(new File(srcFullPath));
    if (!getSrcDirFile().exists()) {
      throw new Exception("Specified input directory does not exist");
    }

    return validationResult;
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    // Set up the handler for identifier assignment
    final IdentifierAssignmentHandler handler =
        newIdentifierAssignmentHandler(getProject().getTerminology());
    handler.setTransactionPerOperation(false);
    handler.beginTransaction();

    // Set up the search handler
    final ComputePreferredNameHandler prefNameHandler =
        getComputePreferredNameHandler(getProject().getTerminology());

    // Count number of added and updated Attributes and Definitions
    // for logging
    int attributeAddCount = 0;
    int attributeUpdateCount = 0;
    int definitionAddCount = 0;
    int definitionUpdateCount = 0;

    try {

      logInfo("  Process attributes.src");
      commitClearBegin();

      //
      // Load the attributes.src file, skipping SEMANTIC_TYPE, CONTEXT,
      // SUBSET_MEMBER, XMAP, XMAPTO, XMAPFROM
      //
      final List<String> lines = loadFileIntoStringList(getSrcDirFile(),
          "attributes.src", null,
          "(.*)(SEMANTIC_TYPE|CONTEXT|SUBSET_MEMBER|XMAP|XMAPTO|XMAPFROM)(.*)");

      // Set the number of steps to the number of lines to be processed
      setSteps(lines.size());

      final String fields[] = new String[14];

      // Each line of relationships.src corresponds to one relationship.
      // Check to make sure the relationship doesn't already exist in the
      // database
      // If it does, skip it.
      // If it does not, add it.
      for (final String line : lines) {

        // Check for a cancelled call once every 100 lines
        if (getStepsCompleted() % 100 == 0) {
          checkCancel();
        }

        FieldedStringTokenizer.split(line, "|", 14, fields);

        // Fields:
        // 0 source_attribute_id
        // 1 sg_id
        // 2 attribute_level
        // 3 attribute_name
        // 4 attribute_value
        // 5 source
        // 6 status
        // 7 tobereleased
        // 8 released
        // 9 suppressible
        // 10 sg_type_1
        // 11 sg_qualifier_1
        // 12 source_atui
        // 13 hashcode

        // e.g.
        // 49|C47666|S|Chemical_Formula|C19H32N2O5.C4H11N|NCI_2016_05E|R|Y|N|N|SOURCE_CUI|NCI_2016_05E||875b4a03f8dedd9de05d6e9e4a440401|

        // Load the terminology that will be assigned to the new Attribute or
        // Definition
        final Terminology setTerminology = getCachedTerminology(fields[5]);
        if (setTerminology == null) {
          logWarnAndUpdate(line,
              "WARNING - terminology not found: " + fields[5] + ".");
          continue;
        }

        // If it's a DEFITION, process the line as a definition instead of an
        // attribute
        if (fields[3].equals("DEFINITION")) {
          // Create the definition
          final Definition newDefinition = new DefinitionJpa();
          newDefinition.setBranch(Branch.ROOT);
          newDefinition.setName(fields[3]);
          newDefinition.setValue(fields[4]);
          newDefinition.setTerminologyId("");
          newDefinition.setTerminology(setTerminology.getTerminology());
          newDefinition.setVersion(setTerminology.getVersion());
          newDefinition.setTimestamp(new Date());
          newDefinition
              .setSuppressible("OYE".contains(fields[8].toUpperCase()));
          newDefinition.setObsolete(fields[9].toUpperCase().equals("O"));
          newDefinition.setPublished(fields[6].toUpperCase().equals("Y"));
          newDefinition.setPublishable(fields[7].toUpperCase().equals("Y"));

          // Load the containing object
          ComponentHasDefinitions containerComponent =
              (ComponentHasDefinitions) getComponent(fields[10], fields[1],
                  getCachedTerminology(fields[11]).getTerminology(), null);
          if (containerComponent == null) {
            logWarnAndUpdate(line,
                "WARNING - could not find Component for type: " + fields[10]
                    + ", terminologyId: " + fields[1] + ", and terminology:"
                    + fields[11]);
            continue;
          }
          Atom atom = null;
          if (containerComponent instanceof Atom) {
            atom = (Atom) containerComponent;
          } else if (containerComponent instanceof AtomClass) {
            final AtomClass atomClass = (AtomClass) containerComponent;
            final List<Atom> atoms =
                prefNameHandler.sortAtoms(atomClass.getAtoms(),
                    getPrecedenceList(getProject().getTerminology(),
                        getProject().getVersion()));
            atom = atoms.get(0);
          } else {
            logWarnAndUpdate(line,
                "WARNING - " + containerComponent.getClass().getName()
                    + " is an unhandled type.");
            continue;
          }

          // Compute definition identity
          final String newDefinitionAtui =
              handler.getTerminologyId(newDefinition, atom);

          // Check to see if attribute with matching ATUI already exists in the
          // database
          final Definition oldDefinition =
              (Definition) getComponent("DEFINITION", newDefinitionAtui,
                  newDefinition.getTerminology(), null);

          // If no definition with the same ATUI exists, create this new
          // definition, and add it to its containing component
          if (oldDefinition == null) {
            newDefinition.getAlternateTerminologyIds()
                .put(getProject().getTerminology(), newDefinitionAtui);
            final Definition newDefinition2 =
                addDefinition(newDefinition, containerComponent);

            definitionAddCount++;
            putComponent(newDefinition2, newDefinitionAtui);

            // Add the definition to component
            atom.getDefinitions().add(newDefinition2);
            updateAtom(atom);
          }
          // If a previous definition with same ATUI exists, load that object.
          else {
            boolean oldDefinitionChanged = false;

            // Attach an ATUI for the definition
            oldDefinition.getAlternateTerminologyIds()
                .put(getProject().getTerminology(), newDefinitionAtui);

            // Update the version
            if (!oldDefinition.getVersion()
                .equals(newDefinition.getVersion())) {
              oldDefinition.setVersion(newDefinition.getVersion());
              oldDefinitionChanged = true;
            }

            // If the existing definition doesn't exactly equal the new one,
            // update obsolete, and suppressible
            if (!oldDefinition.equals(newDefinition)) {
              if (oldDefinition.isObsolete() != newDefinition.isObsolete()) {
                oldDefinition.setObsolete(newDefinition.isObsolete());
                oldDefinitionChanged = true;
              }
              if (oldDefinition.isSuppressible() != newDefinition
                  .isSuppressible()) {
                oldDefinition.setSuppressible(newDefinition.isSuppressible());
                oldDefinitionChanged = true;
              }
            }

            if (oldDefinitionChanged) {
              updateDefinition(oldDefinition, atom);
              definitionUpdateCount++;
            }
          }

        }

        // Otherwise, process the line as an attribute
        else {
          // Create the attribute
          final Attribute newAttribute = new AttributeJpa();
          newAttribute.setBranch(Branch.ROOT);
          newAttribute.setName(fields[3]);
          newAttribute.setValue(fields[4]);
          newAttribute.setTerminologyId(fields[12]);
          newAttribute.setTerminology(setTerminology.getTerminology());
          newAttribute.setVersion(setTerminology.getVersion());
          newAttribute.setTimestamp(new Date());
          newAttribute.setSuppressible("OYE".contains(fields[8].toUpperCase()));
          newAttribute.setObsolete(fields[9].toUpperCase().equals("O"));
          newAttribute.setPublished(fields[6].toUpperCase().equals("Y"));
          newAttribute.setPublishable(fields[7].toUpperCase().equals("Y"));

          // Load the containing object
          final ComponentHasAttributes containerComponent =
              (ComponentHasAttributes) getComponent(fields[10], fields[1],
                  getCachedTerminologyName(fields[11]), null);
          if (containerComponent == null) {
            logWarnAndUpdate(line,
                "WARNING - could not find Component for type: " + fields[10]
                    + ", terminologyId: " + fields[1] + ", and terminology:"
                    + fields[11]);
            continue;
          }

          // Compute attribute identity
          final String newAttributeAtui =
              handler.getTerminologyId(newAttribute, containerComponent);

          // Check to see if attribute with matching ATUI already exists in the
          // database
          final Attribute oldAttribute = (Attribute) getComponent("ATUI",
              newAttributeAtui, newAttribute.getTerminology(), null);

          // If no attribute with the same ATUI exists, create this new
          // Attribute, and add it to its containing component
          if (oldAttribute == null) {
            newAttribute.getAlternateTerminologyIds()
                .put(getProject().getTerminology(), newAttributeAtui);
            final Attribute newAttribute2 =
                addAttribute(newAttribute, containerComponent);

            attributeAddCount++;
            putComponent(newAttribute2, newAttributeAtui);

            // Add the attribute to component
            containerComponent.getAttributes().add(newAttribute2);
            updateComponent(containerComponent);
          }
          // If a previous attribute with same ATUI exists, load that object.
          else {
            boolean oldAttributeChanged = false;

            // Attach an ATUI for the attribute
            oldAttribute.getAlternateTerminologyIds()
                .put(getProject().getTerminology(), newAttributeAtui);

            // Update the version
            if (!oldAttribute.getVersion().equals(newAttribute.getVersion())) {
              oldAttribute.setVersion(newAttribute.getVersion());
              oldAttributeChanged = true;
            }

            // If the existing relationship doesn't exactly equal the new one,
            // update obsolete, and suppressible
            if (!oldAttribute.equals(newAttribute)) {
              if (oldAttribute.isObsolete() != newAttribute.isObsolete()) {
                oldAttribute.setObsolete(newAttribute.isObsolete());
                oldAttributeChanged = true;
              }
              if (oldAttribute.isSuppressible() != newAttribute
                  .isSuppressible()) {
                oldAttribute.setSuppressible(newAttribute.isSuppressible());
                oldAttributeChanged = true;
              }
            }

            if (oldAttributeChanged) {
              updateAttribute(oldAttribute, containerComponent);
              attributeUpdateCount++;
            }
          }
        }

        // Update the progress
        updateProgress();
        handler.silentIntervalCommit(getStepsCompleted(), RootService.logCt,
            RootService.commitCt);
      }

      // Now remove the alternate terminologies for relationships - we don't
      // need them anymore
      clearRelationshipAltTerminologies();

      commitClearBegin();
      handler.commit();

      logInfo("  added attribute count = " + attributeAddCount);
      logInfo("  updated attribute count = " + attributeUpdateCount);
      logInfo("  added definition count = " + definitionAddCount);
      logInfo("  updated definition count = " + definitionUpdateCount);

      logInfo("Finished " + getName());

    } catch (Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      handler.rollback();
      handler.close();
      throw e;
    }

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
    return "Loads and processes an attributes.src file to load Attribute and Definition objects.";
  }

}