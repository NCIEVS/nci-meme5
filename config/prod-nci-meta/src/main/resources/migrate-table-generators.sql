-- =============================================================
-- NCI-MEME5 Tomcat 10 Migration (NM-272): Table Generator Seeding
--
-- Run AFTER deploying the migrated application. The updatedb
-- process will have created any missing generator tables;
-- this script ensures all next_val rows are seeded above the
-- current MAX id in each entity table.
--
-- Safe to run more than once: the ON DUPLICATE KEY UPDATE /
-- GREATEST logic only raises next_val, never lowers it.
-- =============================================================


-- -------------------------------------------------------------
-- 1. table_generator (main content sequence)
--    Used by AbstractComponent entities (atoms, concepts,
--    STYs, relationships, etc.) via UseExistingOrGeneratedId.
--    Sets next_val to MAX(id) + 100 across all content tables
--    that share this sequence (excludes tables with dedicated
--    generators: log, release, report, process, action, wf,
--    users → table_generator_users,
--    type_key_values → table_generator_transformer).
-- -------------------------------------------------------------
UPDATE table_generator
SET next_val = (
  SELECT GREATEST(
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
  ) + 100
)
WHERE sequence_name = 'Entity'
  AND next_val < (
    SELECT GREATEST(
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
      COALESCE((SELECT MAX(id) FROM release_properties),           0),
      COALESCE((SELECT MAX(id) FROM type_key_values),              0),
      COALESCE((SELECT MAX(id) FROM user_preferences),             0),
      COALESCE((SELECT MAX(id) FROM users),                        0)
    ) + 100
  );


-- -------------------------------------------------------------
-- 2. table_generator_log  (LogEntryJpa)
--    Isolated from main sequence to eliminate lock contention.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS table_generator_log (
  sequence_name VARCHAR(255) NOT NULL,
  next_val      BIGINT,
  PRIMARY KEY   (sequence_name)
);

INSERT INTO table_generator_log (sequence_name, next_val)
SELECT 'Entity', COALESCE(MAX(id), 0) + 100 FROM log_entries
ON DUPLICATE KEY UPDATE
  next_val = GREATEST(next_val, VALUES(next_val));


-- -------------------------------------------------------------
-- 3. table_generator_release  (ReleaseInfoJpa)
--    Isolated from main sequence to eliminate lock contention.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS table_generator_release (
  sequence_name VARCHAR(255) NOT NULL,
  next_val      BIGINT,
  PRIMARY KEY   (sequence_name)
);

INSERT INTO table_generator_release (sequence_name, next_val)
SELECT 'Entity', COALESCE(MAX(id), 0) + 100 FROM release_infos
ON DUPLICATE KEY UPDATE
  next_val = GREATEST(next_val, VALUES(next_val));


-- -------------------------------------------------------------
-- 4. table_generator_report
--    Used by ReportJpa, ReportResultJpa, ReportResultItemJpa.
--    Isolated to prevent lock timeout during bulk report
--    generation (potentially thousands of ReportResultItems
--    per report).
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS table_generator_report (
  sequence_name VARCHAR(255) NOT NULL,
  next_val      BIGINT,
  PRIMARY KEY   (sequence_name)
);

INSERT INTO table_generator_report (sequence_name, next_val)
SELECT 'Entity', GREATEST(
  COALESCE((SELECT MAX(id) FROM reports),             0),
  COALESCE((SELECT MAX(id) FROM report_results),      0),
  COALESCE((SELECT MAX(id) FROM report_result_items), 0)
) + 100
ON DUPLICATE KEY UPDATE
  next_val = GREATEST(next_val, VALUES(next_val));


-- -------------------------------------------------------------
-- 5. table_generator_process
--    Used by ProcessConfigJpa, ProcessExecutionJpa,
--    AlgorithmConfigJpa, AlgorithmExecutionJpa.
--    Isolated to prevent contention during algorithm execution.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS table_generator_process (
  sequence_name VARCHAR(255) NOT NULL,
  next_val      BIGINT,
  PRIMARY KEY   (sequence_name)
);

INSERT INTO table_generator_process (sequence_name, next_val)
SELECT 'Entity', GREATEST(
  COALESCE((SELECT MAX(id) FROM process_configs),    0),
  COALESCE((SELECT MAX(id) FROM process_executions), 0),
  COALESCE((SELECT MAX(id) FROM algorithm_configs),  0),
  COALESCE((SELECT MAX(id) FROM algorithm_execs),    0)
) + 100
ON DUPLICATE KEY UPDATE
  next_val = GREATEST(next_val, VALUES(next_val));


-- -------------------------------------------------------------
-- 6. table_generator_users  (UserJpa, UserPreferencesJpa)
--    Pre-existing dedicated table; seeded here for completeness.
-- -------------------------------------------------------------
INSERT INTO table_generator_users (sequence_name, next_val)
SELECT 'Entity', GREATEST(
  COALESCE((SELECT MAX(id) FROM users),            0),
  COALESCE((SELECT MAX(id) FROM user_preferences), 0)
) + 100
ON DUPLICATE KEY UPDATE
  next_val = GREATEST(next_val, VALUES(next_val));


-- -------------------------------------------------------------
-- 7. table_generator_transformer  (TypeKeyValueJpa)
--    Pre-existing dedicated table; seeded here for completeness.
-- -------------------------------------------------------------
INSERT INTO table_generator_transformer (sequence_name, next_val)
SELECT 'Entity', COALESCE(MAX(id), 0) + 100 FROM type_key_values
ON DUPLICATE KEY UPDATE
  next_val = GREATEST(next_val, VALUES(next_val));