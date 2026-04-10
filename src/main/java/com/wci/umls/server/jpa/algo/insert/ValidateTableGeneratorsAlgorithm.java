/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import jakarta.persistence.Query;

import com.wci.umls.server.model.algo.AlgorithmParameter;
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;

/**
 * Algorithm that verifies every table_generator* row has a next_val strictly
 * above the MAX(id) of the entity tables it serves.
 *
 * <p>Run this after any mass insertion (content load, DB restore, or after
 * running migrate-table-generators.sql) to confirm that no generator can
 * produce a duplicate-key collision on the next INSERT.</p>
 *
 * <p>Throws a {@link LocalException} (failing the process) if any generator
 * is stale, listing every offending table in the log.</p>
 */
public class ValidateTableGeneratorsAlgorithm
    extends AbstractInsertMaintReleaseAlgorithm {

  /**
   * Instantiates an empty {@link ValidateTableGeneratorsAlgorithm}.
   *
   * @throws Exception if anything goes wrong
   */
  public ValidateTableGeneratorsAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("VALIDATETABLEGENERATORS");
    setLastModifiedBy("admin");
  }

  /**
   * Check preconditions.
   *
   * @return the validation result
   * @throws Exception the exception
   */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    return new ValidationResultJpa();
  }

  /**
   * Compute.
   *
   * <p>Queries each generator table and compares next_val against the
   * MAX(id) of all entity tables that draw from it.  Logs OK or WARN for
   * each generator, then throws if any are stale.</p>
   *
   * @throws Exception the exception
   */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());
    setMolecularActionFlag(false);

    // Build the check map: generator_table -> SQL that returns a single Long
    // representing the MAX entity id across all tables served by that generator.
    final Map<String, String> checks = new LinkedHashMap<>();

    checks.put("table_generator",
        "SELECT GREATEST("
        + "  COALESCE((SELECT MAX(id) FROM atoms),                        0),"
        + "  COALESCE((SELECT MAX(id) FROM concepts),                     0),"
        + "  COALESCE((SELECT MAX(id) FROM codes),                        0),"
        + "  COALESCE((SELECT MAX(id) FROM descriptors),                  0),"
        + "  COALESCE((SELECT MAX(id) FROM lexical_classes),              0),"
        + "  COALESCE((SELECT MAX(id) FROM string_classes),               0),"
        + "  COALESCE((SELECT MAX(id) FROM semantic_type_components),     0),"
        + "  COALESCE((SELECT MAX(id) FROM atom_relationships),           0),"
        + "  COALESCE((SELECT MAX(id) FROM concept_relationships),        0),"
        + "  COALESCE((SELECT MAX(id) FROM code_relationships),           0),"
        + "  COALESCE((SELECT MAX(id) FROM descriptor_relationships),     0),"
        + "  COALESCE((SELECT MAX(id) FROM component_info_relationships), 0),"
        + "  COALESCE((SELECT MAX(id) FROM atom_transitive_rels),         0),"
        + "  COALESCE((SELECT MAX(id) FROM concept_transitive_rels),      0),"
        + "  COALESCE((SELECT MAX(id) FROM code_transitive_rels),         0),"
        + "  COALESCE((SELECT MAX(id) FROM descriptor_transitive_rels),   0),"
        + "  COALESCE((SELECT MAX(id) FROM atom_tree_positions),          0),"
        + "  COALESCE((SELECT MAX(id) FROM concept_tree_positions),       0),"
        + "  COALESCE((SELECT MAX(id) FROM code_tree_positions),          0),"
        + "  COALESCE((SELECT MAX(id) FROM descriptor_tree_positions),    0),"
        + "  COALESCE((SELECT MAX(id) FROM attributes),                   0),"
        + "  COALESCE((SELECT MAX(id) FROM definitions),                  0),"
        + "  COALESCE((SELECT MAX(id) FROM atom_subsets),                 0),"
        + "  COALESCE((SELECT MAX(id) FROM concept_subsets),              0),"
        + "  COALESCE((SELECT MAX(id) FROM atom_subset_members),          0),"
        + "  COALESCE((SELECT MAX(id) FROM concept_subset_members),       0),"
        + "  COALESCE((SELECT MAX(id) FROM mappings),                     0),"
        + "  COALESCE((SELECT MAX(id) FROM mapsets),                      0),"
        + "  COALESCE((SELECT MAX(id) FROM component_histories),          0),"
        + "  COALESCE((SELECT MAX(id) FROM general_concept_axioms),       0),"
        + "  COALESCE((SELECT MAX(id) FROM atom_notes),                   0),"
        + "  COALESCE((SELECT MAX(id) FROM concept_notes),                0),"
        + "  COALESCE((SELECT MAX(id) FROM code_notes),                   0),"
        + "  COALESCE((SELECT MAX(id) FROM descriptor_notes),             0),"
        + "  COALESCE((SELECT MAX(id) FROM projects),                     0),"
        + "  COALESCE((SELECT MAX(id) FROM precedence_lists),             0),"
        + "  COALESCE((SELECT MAX(id) FROM source_data),                  0),"
        + "  COALESCE((SELECT MAX(id) FROM source_data_files),            0),"
        + "  COALESCE((SELECT MAX(id) FROM source_id_ranges),             0),"
        + "  COALESCE((SELECT MAX(id) FROM terminologies),                0),"
        + "  COALESCE((SELECT MAX(id) FROM root_terminologies),           0),"
        + "  COALESCE((SELECT MAX(id) FROM semantic_types),               0),"
        + "  COALESCE((SELECT MAX(id) FROM attribute_names),              0),"
        + "  COALESCE((SELECT MAX(id) FROM relationship_types),           0),"
        + "  COALESCE((SELECT MAX(id) FROM additional_relationship_types),0),"
        + "  COALESCE((SELECT MAX(id) FROM label_sets),                   0),"
        + "  COALESCE((SELECT MAX(id) FROM languages),                    0),"
        + "  COALESCE((SELECT MAX(id) FROM term_types),                   0),"
        + "  COALESCE((SELECT MAX(id) FROM property_chains),              0),"
        + "  COALESCE((SELECT MAX(id) FROM general_metadata_entries),     0),"
        + "  COALESCE((SELECT MAX(id) FROM citations),                    0),"
        + "  COALESCE((SELECT MAX(id) FROM contact_info),                 0),"
        + "  COALESCE((SELECT MAX(id) FROM release_properties),           0)"
        + ")");

    checks.put("table_generator_log",
        "SELECT COALESCE(MAX(id), 0) FROM log_entries");

    checks.put("table_generator_release",
        "SELECT COALESCE(MAX(id), 0) FROM release_infos");

    checks.put("table_generator_report",
        "SELECT GREATEST("
        + "  COALESCE((SELECT MAX(id) FROM reports),             0),"
        + "  COALESCE((SELECT MAX(id) FROM report_results),      0),"
        + "  COALESCE((SELECT MAX(id) FROM report_result_items), 0)"
        + ")");

    checks.put("table_generator_process",
        "SELECT GREATEST("
        + "  COALESCE((SELECT MAX(id) FROM process_configs),    0),"
        + "  COALESCE((SELECT MAX(id) FROM process_executions), 0),"
        + "  COALESCE((SELECT MAX(id) FROM algorithm_configs),  0),"
        + "  COALESCE((SELECT MAX(id) FROM algorithm_execs),    0)"
        + ")");

    checks.put("table_generator_action",
        "SELECT GREATEST("
        + "  COALESCE((SELECT MAX(id) FROM atomic_actions),    0),"
        + "  COALESCE((SELECT MAX(id) FROM molecular_actions), 0)"
        + ")");

    checks.put("table_generator_wf",
        "SELECT GREATEST("
        + "  COALESCE((SELECT MAX(id) FROM workflow_configs),        0),"
        + "  COALESCE((SELECT MAX(id) FROM workflow_epochs),         0),"
        + "  COALESCE((SELECT MAX(id) FROM workflow_bin_definitions),0),"
        + "  COALESCE((SELECT MAX(id) FROM workflow_bins),           0),"
        + "  COALESCE((SELECT MAX(id) FROM worklists),               0),"
        + "  COALESCE((SELECT MAX(id) FROM worklist_notes),          0),"
        + "  COALESCE((SELECT MAX(id) FROM checklists),              0),"
        + "  COALESCE((SELECT MAX(id) FROM checklist_notes),         0),"
        + "  COALESCE((SELECT MAX(id) FROM tracking_records),        0)"
        + ")");

    checks.put("table_generator_users",
        "SELECT GREATEST("
        + "  COALESCE((SELECT MAX(id) FROM users),            0),"
        + "  COALESCE((SELECT MAX(id) FROM user_preferences), 0)"
        + ")");

    checks.put("table_generator_transformer",
        "SELECT COALESCE(MAX(id), 0) FROM type_key_values");

    // Run all checks and collect failures
    final List<String> failures = new ArrayList<>();

    for (final Map.Entry<String, String> entry : checks.entrySet()) {
      final String generatorTable = entry.getKey();
      final String maxIdSql = entry.getValue();

      // Fetch next_val from the generator table
      final Query nextValQuery = getEntityManager().createNativeQuery(
          "SELECT next_val FROM " + generatorTable
          + " WHERE sequence_name = 'Entity'");
      @SuppressWarnings("unchecked")
      final List<Object> nextValRows = nextValQuery.getResultList();

      if (nextValRows.isEmpty()) {
        final String msg = generatorTable
            + " — no 'Entity' row found in generator table";
        logError("  WARN: " + msg);
        failures.add(msg);
        continue;
      }

      final long nextVal = ((Number) nextValRows.get(0)).longValue();

      // Fetch MAX(id) across entity tables served by this generator
      final Query maxIdQuery =
          getEntityManager().createNativeQuery(maxIdSql);
      final long maxEntityId =
          ((Number) maxIdQuery.getSingleResult()).longValue();

      if (nextVal > maxEntityId) {
        logInfo("  OK:   " + generatorTable
            + " — next_val=" + nextVal
            + "  max_entity_id=" + maxEntityId);
      } else {
        final String msg = generatorTable
            + " — next_val=" + nextVal
            + " is NOT above max_entity_id=" + maxEntityId
            + "; run migrate-table-generators.sql to reseed";
        logError("  WARN: " + msg);
        failures.add(msg);
      }
    }

    if (!failures.isEmpty()) {
      throw new LocalException(
          "Table generator validation failed for "
          + failures.size() + " generator(s):\n  "
          + String.join("\n  ", failures));
    }

    logInfo("All " + checks.size() + " table generators are properly seeded.");
    logInfo("Finished " + getName());
  }

  /**
   * Reset.
   *
   * @throws Exception the exception
   */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // n/a - read-only validation, nothing to undo
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
    return super.getParameters();
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Validates that all table_generator* next_val entries are above "
        + "the MAX(id) of their respective entity tables.";
  }
}
