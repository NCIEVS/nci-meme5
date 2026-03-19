-- =============================================================
-- NCI-MEME5 Table Generator Validation
--
-- Run after any mass insertion (content load, restore from dump,
-- migrate-table-generators.sql, etc.) to confirm that every
-- generator table's next_val is strictly above the MAX(id)
-- currently stored in its entity tables.
--
-- A row with status = 'WARN' means the generator would produce
-- a duplicate-key collision on the next INSERT.  Run
-- migrate-table-generators.sql to fix.
--
-- Safe to run at any time; read-only.
-- =============================================================

SELECT
  generator_table,
  next_val,
  max_entity_id,
  IF(next_val > max_entity_id,
     'OK',
     CONCAT('WARN — next_val (', next_val, ') <= max_id (', max_entity_id, ')'))
    AS status
FROM (

  -- -----------------------------------------------------------
  -- 1. table_generator  (main content: AbstractComponent +
  --    meta entities via EntityIdGen)
  -- -----------------------------------------------------------
  SELECT
    'table_generator' AS generator_table,
    (SELECT next_val FROM table_generator WHERE sequence_name = 'Entity') AS next_val,
    GREATEST(
      COALESCE((SELECT MAX(id) FROM atoms),                        0),
      COALESCE((SELECT MAX(id) FROM concepts),                     0),
      COALESCE((SELECT MAX(id) FROM codes),                        0),
      COALESCE((SELECT MAX(id) FROM descriptors),                  0),
      COALESCE((SELECT MAX(id) FROM lexical_classes),              0),
      COALESCE((SELECT MAX(id) FROM string_classes),               0),
      COALESCE((SELECT MAX(id) FROM semantic_type_components),     0),
      COALESCE((SELECT MAX(id) FROM atom_relationships),           0),
      COALESCE((SELECT MAX(id) FROM concept_relationships),        0),
      COALESCE((SELECT MAX(id) FROM code_relationships),           0),
      COALESCE((SELECT MAX(id) FROM descriptor_relationships),     0),
      COALESCE((SELECT MAX(id) FROM component_info_relationships), 0),
      COALESCE((SELECT MAX(id) FROM atom_transitive_rels),         0),
      COALESCE((SELECT MAX(id) FROM concept_transitive_rels),      0),
      COALESCE((SELECT MAX(id) FROM code_transitive_rels),         0),
      COALESCE((SELECT MAX(id) FROM descriptor_transitive_rels),   0),
      COALESCE((SELECT MAX(id) FROM atom_tree_positions),          0),
      COALESCE((SELECT MAX(id) FROM concept_tree_positions),       0),
      COALESCE((SELECT MAX(id) FROM code_tree_positions),          0),
      COALESCE((SELECT MAX(id) FROM descriptor_tree_positions),    0),
      COALESCE((SELECT MAX(id) FROM attributes),                   0),
      COALESCE((SELECT MAX(id) FROM definitions),                  0),
      COALESCE((SELECT MAX(id) FROM atom_subsets),                 0),
      COALESCE((SELECT MAX(id) FROM concept_subsets),              0),
      COALESCE((SELECT MAX(id) FROM atom_subset_members),          0),
      COALESCE((SELECT MAX(id) FROM concept_subset_members),       0),
      COALESCE((SELECT MAX(id) FROM mappings),                     0),
      COALESCE((SELECT MAX(id) FROM mapsets),                      0),
      COALESCE((SELECT MAX(id) FROM component_histories),          0),
      COALESCE((SELECT MAX(id) FROM general_concept_axioms),       0),
      COALESCE((SELECT MAX(id) FROM atom_notes),                   0),
      COALESCE((SELECT MAX(id) FROM concept_notes),                0),
      COALESCE((SELECT MAX(id) FROM code_notes),                   0),
      COALESCE((SELECT MAX(id) FROM descriptor_notes),             0),
      COALESCE((SELECT MAX(id) FROM projects),                     0),
      COALESCE((SELECT MAX(id) FROM precedence_lists),             0),
      COALESCE((SELECT MAX(id) FROM source_data),                  0),
      COALESCE((SELECT MAX(id) FROM source_data_files),            0),
      COALESCE((SELECT MAX(id) FROM source_id_ranges),             0),
      COALESCE((SELECT MAX(id) FROM terminologies),                0),
      COALESCE((SELECT MAX(id) FROM root_terminologies),           0),
      COALESCE((SELECT MAX(id) FROM semantic_types),               0),
      COALESCE((SELECT MAX(id) FROM attribute_names),              0),
      COALESCE((SELECT MAX(id) FROM relationship_types),           0),
      COALESCE((SELECT MAX(id) FROM additional_relationship_types),0),
      COALESCE((SELECT MAX(id) FROM label_sets),                   0),
      COALESCE((SELECT MAX(id) FROM languages),                    0),
      COALESCE((SELECT MAX(id) FROM term_types),                   0),
      COALESCE((SELECT MAX(id) FROM property_chains),              0),
      COALESCE((SELECT MAX(id) FROM general_metadata_entries),     0),
      COALESCE((SELECT MAX(id) FROM citations),                    0),
      COALESCE((SELECT MAX(id) FROM contact_info),                 0),
      COALESCE((SELECT MAX(id) FROM release_properties),           0)
    ) AS max_entity_id

  UNION ALL

  -- -----------------------------------------------------------
  -- 2. table_generator_log  (LogEntryJpa → log_entries)
  -- -----------------------------------------------------------
  SELECT
    'table_generator_log',
    (SELECT next_val FROM table_generator_log WHERE sequence_name = 'Entity'),
    COALESCE((SELECT MAX(id) FROM log_entries), 0)

  UNION ALL

  -- -----------------------------------------------------------
  -- 3. table_generator_release  (ReleaseInfoJpa → release_infos)
  -- -----------------------------------------------------------
  SELECT
    'table_generator_release',
    (SELECT next_val FROM table_generator_release WHERE sequence_name = 'Entity'),
    COALESCE((SELECT MAX(id) FROM release_infos), 0)

  UNION ALL

  -- -----------------------------------------------------------
  -- 4. table_generator_report
  --    (ReportJpa, ReportResultJpa, ReportResultItemJpa)
  -- -----------------------------------------------------------
  SELECT
    'table_generator_report',
    (SELECT next_val FROM table_generator_report WHERE sequence_name = 'Entity'),
    GREATEST(
      COALESCE((SELECT MAX(id) FROM reports),             0),
      COALESCE((SELECT MAX(id) FROM report_results),      0),
      COALESCE((SELECT MAX(id) FROM report_result_items), 0)
    )

  UNION ALL

  -- -----------------------------------------------------------
  -- 5. table_generator_process
  --    (ProcessConfigJpa, ProcessExecutionJpa,
  --     AlgorithmConfigJpa, AlgorithmExecutionJpa)
  -- -----------------------------------------------------------
  SELECT
    'table_generator_process',
    (SELECT next_val FROM table_generator_process WHERE sequence_name = 'Entity'),
    GREATEST(
      COALESCE((SELECT MAX(id) FROM process_configs),    0),
      COALESCE((SELECT MAX(id) FROM process_executions), 0),
      COALESCE((SELECT MAX(id) FROM algorithm_configs),  0),
      COALESCE((SELECT MAX(id) FROM algorithm_execs),    0)
    )

  UNION ALL

  -- -----------------------------------------------------------
  -- 6. table_generator_action
  --    (AtomicActionJpa → atomic_actions,
  --     MolecularActionJpa → molecular_actions)
  -- -----------------------------------------------------------
  SELECT
    'table_generator_action',
    (SELECT next_val FROM table_generator_action WHERE sequence_name = 'Entity'),
    GREATEST(
      COALESCE((SELECT MAX(id) FROM atomic_actions),    0),
      COALESCE((SELECT MAX(id) FROM molecular_actions), 0)
    )

  UNION ALL

  -- -----------------------------------------------------------
  -- 7. table_generator_wf  (workflow entities)
  -- -----------------------------------------------------------
  SELECT
    'table_generator_wf',
    (SELECT next_val FROM table_generator_wf WHERE sequence_name = 'Entity'),
    GREATEST(
      COALESCE((SELECT MAX(id) FROM workflow_configs),       0),
      COALESCE((SELECT MAX(id) FROM workflow_epochs),        0),
      COALESCE((SELECT MAX(id) FROM workflow_bin_definitions),0),
      COALESCE((SELECT MAX(id) FROM workflow_bins),          0),
      COALESCE((SELECT MAX(id) FROM worklists),              0),
      COALESCE((SELECT MAX(id) FROM worklist_notes),         0),
      COALESCE((SELECT MAX(id) FROM checklists),             0),
      COALESCE((SELECT MAX(id) FROM checklist_notes),        0),
      COALESCE((SELECT MAX(id) FROM tracking_records),       0)
    )

  UNION ALL

  -- -----------------------------------------------------------
  -- 8. table_generator_users
  --    (UserJpa → users, UserPreferencesJpa → user_preferences)
  -- -----------------------------------------------------------
  SELECT
    'table_generator_users',
    (SELECT next_val FROM table_generator_users WHERE sequence_name = 'Entity'),
    GREATEST(
      COALESCE((SELECT MAX(id) FROM users),            0),
      COALESCE((SELECT MAX(id) FROM user_preferences), 0)
    )

  UNION ALL

  -- -----------------------------------------------------------
  -- 9. table_generator_transformer  (TypeKeyValueJpa)
  -- -----------------------------------------------------------
  SELECT
    'table_generator_transformer',
    (SELECT next_val FROM table_generator_transformer WHERE sequence_name = 'Entity'),
    COALESCE((SELECT MAX(id) FROM type_key_values), 0)

) AS checks
ORDER BY status DESC, generator_table;
