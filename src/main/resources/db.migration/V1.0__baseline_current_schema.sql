--
-- Baseline schema for a fresh MEME MySQL database.
-- Generated from the local ncimdb schema with mysqldump --no-data.
-- Existing populated legacy databases should use adminFlywayBaseline instead
-- of running this schema creation migration.
--

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `additional_relationship_types` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `abbreviation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `expandedForm` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `asymmetric` bit(1) NOT NULL,
  `domainId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `equivalentClasses` bit(1) NOT NULL,
  `existentialQuantification` bit(1) NOT NULL,
  `functional` bit(1) NOT NULL,
  `groupingType` bit(1) NOT NULL,
  `hierarchical` bit(1) NOT NULL,
  `inverseFunctional` bit(1) NOT NULL,
  `irreflexive` bit(1) NOT NULL,
  `rangeId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `reflexive` bit(1) NOT NULL,
  `symmetric` bit(1) NOT NULL,
  `transitive` bit(1) NOT NULL,
  `universalQuantification` bit(1) NOT NULL,
  `equivalentType_id` bigint DEFAULT NULL,
  `inverse_id` bigint DEFAULT NULL,
  `superType_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_q5h8kk5llplods90t1ingp9ww` (`abbreviation`,`terminology`,`id`),
  UNIQUE KEY `UKq5h8kk5llplods90t1ingp9ww` (`abbreviation`,`terminology`,`id`),
  KEY `FK_5e6al2f1yisl97ikmkbu7v70c` (`equivalentType_id`),
  KEY `FK_s5k9y1pd87r39sjfc8h4kvhaj` (`inverse_id`),
  KEY `FK_gkswep8uu1pop8wk9u79ed94` (`superType_id`),
  CONSTRAINT `FK_5e6al2f1yisl97ikmkbu7v70c` FOREIGN KEY (`equivalentType_id`) REFERENCES `additional_relationship_types` (`id`),
  CONSTRAINT `FK_gkswep8uu1pop8wk9u79ed94` FOREIGN KEY (`superType_id`) REFERENCES `additional_relationship_types` (`id`),
  CONSTRAINT `FK_s5k9y1pd87r39sjfc8h4kvhaj` FOREIGN KEY (`inverse_id`) REFERENCES `additional_relationship_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `algorithm_configs` (
  `id` bigint NOT NULL,
  `algorithmKey` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `enabled` bit(1) NOT NULL,
  `project_id` bigint NOT NULL,
  `process_id` bigint NOT NULL,
  `steps_ORDER` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_smq8td6k68gv79bfck8ncyawc` (`project_id`),
  KEY `FK_6jkfuxs5xdevup0mk0qex81fp` (`process_id`),
  CONSTRAINT `FK_6jkfuxs5xdevup0mk0qex81fp` FOREIGN KEY (`process_id`) REFERENCES `process_configs` (`id`),
  CONSTRAINT `FK_smq8td6k68gv79bfck8ncyawc` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `algorithm_execs` (
  `id` bigint NOT NULL,
  `algorithmKey` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `activityId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `algorithmConfigId` bigint NOT NULL,
  `failDate` datetime DEFAULT NULL,
  `finishDate` datetime DEFAULT NULL,
  `startDate` datetime DEFAULT NULL,
  `warning` bit(1) NOT NULL,
  `project_id` bigint NOT NULL,
  `process_id` bigint NOT NULL,
  `steps_ORDER` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_76i85nrypuxr5ywlwbwl835t1` (`project_id`),
  KEY `FK_4wyp02ari30f8801hwskuao3l` (`process_id`),
  CONSTRAINT `FK_4wyp02ari30f8801hwskuao3l` FOREIGN KEY (`process_id`) REFERENCES `process_executions` (`id`),
  CONSTRAINT `FK_76i85nrypuxr5ywlwbwl835t1` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `algorithmconfigjpa_properties` (
  `AlgorithmConfigJpa_id` bigint NOT NULL,
  `properties` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `properties_KEY` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`AlgorithmConfigJpa_id`,`properties_KEY`),
  CONSTRAINT `FK_pbcdc3434mrbppcbltmdohwfd` FOREIGN KEY (`AlgorithmConfigJpa_id`) REFERENCES `algorithm_configs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `algorithmexecutionjpa_properties` (
  `AlgorithmExecutionJpa_id` bigint NOT NULL,
  `properties` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `properties_KEY` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`AlgorithmExecutionJpa_id`,`properties_KEY`),
  CONSTRAINT `FK_ikhx6vtcjyivh28e9twokfrdb` FOREIGN KEY (`AlgorithmExecutionJpa_id`) REFERENCES `algorithm_execs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ambig_concepts` AS SELECT 
 1 AS `conceptId1`,
 1 AS `conceptId2`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_identity` (
  `id` bigint NOT NULL,
  `codeId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `conceptId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `descriptorId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `stringClassId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `termType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_o4hgadwxb5erh5lop925mr2rf` (`stringClassId`,`terminology`,`terminologyId`,`id`),
  UNIQUE KEY `UK_cfvk7p01d9kjftgqfqvhlie4f` (`conceptId`,`terminology`,`terminologyId`,`id`),
  UNIQUE KEY `UK_j122lndst35w26pn5usiuvhxq` (`descriptorId`,`terminology`,`terminologyId`,`id`),
  UNIQUE KEY `UK_ls5lpbg97uqdnlot1x3k3qdfm` (`codeId`,`terminology`,`terminologyId`,`id`),
  UNIQUE KEY `UKo4hgadwxb5erh5lop925mr2rf` (`stringClassId`,`terminology`,`terminologyId`,`id`),
  UNIQUE KEY `UKcfvk7p01d9kjftgqfqvhlie4f` (`conceptId`,`terminology`,`terminologyId`,`id`),
  UNIQUE KEY `UKj122lndst35w26pn5usiuvhxq` (`descriptorId`,`terminology`,`terminologyId`,`id`),
  UNIQUE KEY `UKls5lpbg97uqdnlot1x3k3qdfm` (`codeId`,`terminology`,`terminologyId`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_notes` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `note` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `atom_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_55ilaxdpr7sj2fqa86oy0w82c` (`atom_id`),
  CONSTRAINT `FK_55ilaxdpr7sj2fqa86oy0w82c` FOREIGN KEY (`atom_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_relationships` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `assertedDirection` bit(1) NOT NULL,
  `relGroup` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `hierarchical` bit(1) NOT NULL,
  `inferred` bit(1) NOT NULL,
  `relationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `stated` bit(1) NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `from_id` bigint NOT NULL,
  `to_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_h5022m5q323e5vxnf0vyv801` (`terminologyId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UKh5022m5q323e5vxnf0vyv801` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_gx09a9bsnkaechc49hw2n6j8b` (`from_id`),
  KEY `FK_8olp9m2ohjh61iie72bl73v9p` (`to_id`),
  KEY `x_atom_rels_1` (`workflowStatus`),
  KEY `x_ar_t` (`terminology`),
  CONSTRAINT `FK_8olp9m2ohjh61iie72bl73v9p` FOREIGN KEY (`to_id`) REFERENCES `atoms` (`id`),
  CONSTRAINT `FK_gx09a9bsnkaechc49hw2n6j8b` FOREIGN KEY (`from_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_relationships_attributes` (
  `atom_relationships_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_72995yo62swv40lqwdgfarlk3` (`attributes_id`),
  KEY `FK_8nj2jbedq7lm9wdst0m41h6be` (`atom_relationships_id`),
  CONSTRAINT `FK_72995yo62swv40lqwdgfarlk3` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_8nj2jbedq7lm9wdst0m41h6be` FOREIGN KEY (`atom_relationships_id`) REFERENCES `atom_relationships` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_subset_members` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `member_id` bigint NOT NULL,
  `subset_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_5j339if69sgss47cp9f1eueyd` (`terminologyId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK5j339if69sgss47cp9f1eueyd` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_t8kfy2r4wxo68ygat1gyktsko` (`member_id`),
  KEY `FK_oon5x47dxgaipdtviesxtmg6l` (`subset_id`),
  KEY `x_asm_1` (`terminologyId`),
  CONSTRAINT `FK_oon5x47dxgaipdtviesxtmg6l` FOREIGN KEY (`subset_id`) REFERENCES `atom_subsets` (`id`),
  CONSTRAINT `FK_t8kfy2r4wxo68ygat1gyktsko` FOREIGN KEY (`member_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_subset_members_attributes` (
  `atom_subset_members_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_ryesbpbcb4boy4rxv24bux6wn` (`attributes_id`),
  KEY `FK_iqaks0t6te4oa69vtssd4sj27` (`atom_subset_members_id`),
  CONSTRAINT `FK_iqaks0t6te4oa69vtssd4sj27` FOREIGN KEY (`atom_subset_members_id`) REFERENCES `atom_subset_members` (`id`),
  CONSTRAINT `FK_ryesbpbcb4boy4rxv24bux6wn` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_subsets` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branchedTo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `description` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ak3fii22vi7dksth7p93mmt6m` (`terminologyId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UKak3fii22vi7dksth7p93mmt6m` (`terminologyId`,`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_subsets_attributes` (
  `atom_subsets_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_9d82ck448cb5tya3ati2kjpte` (`attributes_id`),
  KEY `FK_efbnjto98w9xg0di590yxxxt5` (`atom_subsets_id`),
  CONSTRAINT `FK_9d82ck448cb5tya3ati2kjpte` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_efbnjto98w9xg0di590yxxxt5` FOREIGN KEY (`atom_subsets_id`) REFERENCES `atom_subsets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_transitive_rels` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `depth` int NOT NULL,
  `subType_id` bigint NOT NULL,
  `superType_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_gy1jghlx5hngo0i09q0h0wxwi` (`terminologyId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UKgy1jghlx5hngo0i09q0h0wxwi` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_47bboxg1ackf28kbn2uisl24j` (`subType_id`),
  KEY `FK_s92ulnbytas7c1dt373a2xvlm` (`superType_id`),
  CONSTRAINT `FK_47bboxg1ackf28kbn2uisl24j` FOREIGN KEY (`subType_id`) REFERENCES `atoms` (`id`),
  CONSTRAINT `FK_s92ulnbytas7c1dt373a2xvlm` FOREIGN KEY (`superType_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_transitive_rels_attributes` (
  `atom_transitive_rels_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_o86pdt7dxvfux2880kq0crv33` (`attributes_id`),
  KEY `FK_56cul38y5785uh10xp8ggs4v3` (`atom_transitive_rels_id`),
  CONSTRAINT `FK_56cul38y5785uh10xp8ggs4v3` FOREIGN KEY (`atom_transitive_rels_id`) REFERENCES `atom_transitive_rels` (`id`),
  CONSTRAINT `FK_o86pdt7dxvfux2880kq0crv33` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_tree_positions` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ancestorPath` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `childCt` int NOT NULL,
  `descendantCt` int NOT NULL,
  `node_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_b538wdkymyu2hadqg1aj06li3` (`terminologyId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UKb538wdkymyu2hadqg1aj06li3` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_psmhjog9mddx3udldxpxce3ww` (`node_id`),
  KEY `x_atr_t` (`terminology`),
  CONSTRAINT `FK_psmhjog9mddx3udldxpxce3ww` FOREIGN KEY (`node_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atom_tree_positions_attributes` (
  `atom_tree_positions_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_d8k4l3y7w0eruisigw9ao0qk1` (`attributes_id`),
  KEY `FK_dx81eishuwr3bww4cj2bs2uy5` (`atom_tree_positions_id`),
  CONSTRAINT `FK_d8k4l3y7w0eruisigw9ao0qk1` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_dx81eishuwr3bww4cj2bs2uy5` FOREIGN KEY (`atom_tree_positions_id`) REFERENCES `atom_tree_positions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atomic_actions` (
  `id` bigint NOT NULL,
  `className` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `collectionClassName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `field` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `idType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `newValue` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `objectId` bigint NOT NULL,
  `oldValue` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `molecularAction_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_pr6vl7ojvbgmqkigyycsgfq1r` (`objectId`,`id`),
  UNIQUE KEY `UKpr6vl7ojvbgmqkigyycsgfq1r` (`objectId`,`id`),
  KEY `FK_77cohj1fobfavn90404kxllrw` (`molecularAction_id`),
  CONSTRAINT `FK_77cohj1fobfavn90404kxllrw` FOREIGN KEY (`molecularAction_id`) REFERENCES `molecular_actions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atomjpa_alternateterminologyids` (
  `AtomJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`AtomJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_2eg3b14g7rpy7ke4g910xhc27` FOREIGN KEY (`AtomJpa_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atomjpa_conceptterminologyids` (
  `AtomJpa_id` bigint NOT NULL,
  `conceptTerminologyIds` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `conceptTerminologyIds_KEY` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`AtomJpa_id`,`conceptTerminologyIds_KEY`),
  KEY `idx_atomjpa_conceptterminologyids` (`conceptTerminologyIds`,`conceptTerminologyIds_KEY`),
  CONSTRAINT `FK_otw2gbodegenm0niv9oncc9ys` FOREIGN KEY (`AtomJpa_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atomrelationshipjpa_alternateterminologyids` (
  `AtomRelationshipJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`AtomRelationshipJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_4ng42427g2xcp5f630mr7g1ig` FOREIGN KEY (`AtomRelationshipJpa_id`) REFERENCES `atom_relationships` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atoms` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `codeId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `conceptId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `descriptorId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `language` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `lastPublishedRank` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lexicalClassId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `lowerNameHash` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `name` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `stringClassId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `termType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `rxcui` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_72cko3y2iycrmxavm2jx9xega` (`terminologyId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK_d9va8q3m374s2mcm9id4v7117` (`conceptId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK_596cpv3vpga7jgvxtlb7as9fs` (`codeId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK_taman984wgfvw74v3mc7oif2g` (`descriptorId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK_svmiiue8ja2avclyk2qda9s0h` (`lexicalClassId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK_40vnuucymnhr3e2yjo833f0ps` (`stringClassId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK_saika60ft030tmdx8wpo306un` (`terminology`,`version`,`id`),
  UNIQUE KEY `UK72cko3y2iycrmxavm2jx9xega` (`terminologyId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UKd9va8q3m374s2mcm9id4v7117` (`conceptId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK596cpv3vpga7jgvxtlb7as9fs` (`codeId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UKtaman984wgfvw74v3mc7oif2g` (`descriptorId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UKsvmiiue8ja2avclyk2qda9s0h` (`lexicalClassId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK40vnuucymnhr3e2yjo833f0ps` (`stringClassId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UKsaika60ft030tmdx8wpo306un` (`terminology`,`version`,`id`),
  UNIQUE KEY `UK_47unimbunc3e1pig2jb1gxweb` (`lowerNameHash`,`conceptId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK47unimbunc3e1pig2jb1gxweb` (`lowerNameHash`,`conceptId`,`terminology`,`version`,`id`),
  KEY `x_atoms_1` (`workflowStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atoms_attributes` (
  `atoms_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_iadpuqcn49xdl8fgahrqxt93o` (`attributes_id`),
  KEY `FK_ns7vn9hvd59dgepkt0135l9x7` (`atoms_id`),
  CONSTRAINT `FK_iadpuqcn49xdl8fgahrqxt93o` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_ns7vn9hvd59dgepkt0135l9x7` FOREIGN KEY (`atoms_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atoms_component_histories` (
  `atoms_id` bigint NOT NULL,
  `componentHistories_id` bigint NOT NULL,
  UNIQUE KEY `UK_2fpng52c5p9i8yxbgkpebpp5n` (`componentHistories_id`),
  KEY `FK_fopblvf7rwsva6v90lqfefitm` (`atoms_id`),
  CONSTRAINT `FK_2fpng52c5p9i8yxbgkpebpp5n` FOREIGN KEY (`componentHistories_id`) REFERENCES `component_histories` (`id`),
  CONSTRAINT `FK_fopblvf7rwsva6v90lqfefitm` FOREIGN KEY (`atoms_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atoms_definitions` (
  `atoms_id` bigint NOT NULL,
  `definitions_id` bigint NOT NULL,
  UNIQUE KEY `UK_6w7o1bfare2xgi0rdnbymknfp` (`definitions_id`),
  KEY `FK_a55dntdrx3sevdb3bsrhpmi2x` (`atoms_id`),
  CONSTRAINT `FK_6w7o1bfare2xgi0rdnbymknfp` FOREIGN KEY (`definitions_id`) REFERENCES `definitions` (`id`),
  CONSTRAINT `FK_a55dntdrx3sevdb3bsrhpmi2x` FOREIGN KEY (`atoms_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `atomsubsetjpa_alternateterminologyids` (
  `AtomSubsetJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`AtomSubsetJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_h9d15vkipskewyiqjqcxeyi7k` FOREIGN KEY (`AtomSubsetJpa_id`) REFERENCES `atom_subsets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attribute_identity` (
  `id` bigint NOT NULL,
  `componentId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `componentTerminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `componentType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `hashcode` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_8y6rw7kunkyr1qxoonmgrkcjf` (`componentId`,`componentTerminology`,`componentType`,`id`),
  UNIQUE KEY `UK8y6rw7kunkyr1qxoonmgrkcjf` (`componentId`,`componentTerminology`,`componentType`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attribute_names` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `abbreviation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `expandedForm` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `annotation` bit(1) NOT NULL,
  `domainId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `existentialQuantification` bit(1) NOT NULL,
  `functional` bit(1) NOT NULL,
  `rangeId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `universalQuantification` bit(1) NOT NULL,
  `equivalentName_id` bigint DEFAULT NULL,
  `superName_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_tom9u9st4jlwbtt1g17y10d6n` (`abbreviation`,`terminology`,`id`),
  KEY `FK_5wvfbyve4bbkwbo3og91pidh3` (`equivalentName_id`),
  KEY `FK_5d2ccb8thutf5w2k4alxwmyax` (`superName_id`),
  CONSTRAINT `FK_5d2ccb8thutf5w2k4alxwmyax` FOREIGN KEY (`superName_id`) REFERENCES `attribute_names` (`id`),
  CONSTRAINT `FK_5wvfbyve4bbkwbo3og91pidh3` FOREIGN KEY (`equivalentName_id`) REFERENCES `attribute_names` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attributejpa_alternateterminologyids` (
  `AttributeJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`AttributeJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_2s0tdvmp7vtvl1ptah3jtk5fk` FOREIGN KEY (`AttributeJpa_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attributes` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `value` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_s38tii2p04scf5xu3fo3qjs98` (`terminologyId`,`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `auis_m4` AS SELECT 
 1 AS `atom_id`,
 1 AS `aui`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `checklist_notes` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `note` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `checklist_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_9g7y7kggt8pbbhcqst4q5wflu` (`checklist_id`),
  CONSTRAINT `FK_9g7y7kggt8pbbhcqst4q5wflu` FOREIGN KEY (`checklist_id`) REFERENCES `checklists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `checklists` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `description` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_7u0yg7ap7cyjgxiq6s7bypcuw` (`name`,`project_id`),
  KEY `FK_nh1kqw3p3uls7cdp4xe17kexs` (`project_id`),
  CONSTRAINT `FK_nh1kqw3p3uls7cdp4xe17kexs` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `checklists_tracking_records` (
  `checklists_id` bigint NOT NULL,
  `trackingRecords_id` bigint NOT NULL,
  UNIQUE KEY `UK_26k8kl94khww1y7lfw100s98e` (`trackingRecords_id`),
  KEY `FK_byl2rvq5trag27qf3xf4g1ykw` (`checklists_id`),
  CONSTRAINT `FK_26k8kl94khww1y7lfw100s98e` FOREIGN KEY (`trackingRecords_id`) REFERENCES `tracking_records` (`id`),
  CONSTRAINT `FK_byl2rvq5trag27qf3xf4g1ykw` FOREIGN KEY (`checklists_id`) REFERENCES `checklists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `citations` (
  `id` bigint NOT NULL,
  `address` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `author` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `availabilityStatement` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `contentDesignator` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `dateOfPublication` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `dateOfRevision` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `edition` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `editor` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `extent` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `language` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `location` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `mediumDesignator` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `notes` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `organization` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `placeOfPublication` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `publisher` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `series` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `unstructuredValue` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `classes_m4` AS SELECT 
 1 AS `atom_d`,
 1 AS `name`,
 1 AS `terminology`,
 1 AS `version`,
 1 AS `publishable`,
 1 AS `sui`,
 1 AS `lui`,
 1 AS `code`,
 1 AS `scui`,
 1 AS `sdui`,
 1 AS `concept_id`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_notes` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `note` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `code_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_jo3o3jivgwj2q87m5ghoc1ly2` (`code_id`),
  CONSTRAINT `FK_jo3o3jivgwj2q87m5ghoc1ly2` FOREIGN KEY (`code_id`) REFERENCES `codes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_relationships` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `assertedDirection` bit(1) NOT NULL,
  `relGroup` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `hierarchical` bit(1) NOT NULL,
  `inferred` bit(1) NOT NULL,
  `relationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `stated` bit(1) NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `from_id` bigint NOT NULL,
  `to_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_q8xujmdxwwvfhgptinbmposaw` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_jv00pgcnfgk5xbci9r4dedkii` (`from_id`),
  KEY `FK_rgxcjq1u9t4fc65vilbljes6s` (`to_id`),
  CONSTRAINT `FK_jv00pgcnfgk5xbci9r4dedkii` FOREIGN KEY (`from_id`) REFERENCES `codes` (`id`),
  CONSTRAINT `FK_rgxcjq1u9t4fc65vilbljes6s` FOREIGN KEY (`to_id`) REFERENCES `codes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_relationships_attributes` (
  `code_relationships_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_2t15wxomnbk89kgwbh17lwh68` (`attributes_id`),
  KEY `FK_jonlj9tu2g2aii46vkwa8np1o` (`code_relationships_id`),
  CONSTRAINT `FK_2t15wxomnbk89kgwbh17lwh68` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_jonlj9tu2g2aii46vkwa8np1o` FOREIGN KEY (`code_relationships_id`) REFERENCES `code_relationships` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_transitive_rels` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `depth` int NOT NULL,
  `subType_id` bigint NOT NULL,
  `superType_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_cmuton5dn58e38reupiqk61in` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_lv56tobhuavv8c9g4vw40ifdg` (`subType_id`),
  KEY `FK_f1j806h6moh1fxvd8tcxuopd7` (`superType_id`),
  CONSTRAINT `FK_f1j806h6moh1fxvd8tcxuopd7` FOREIGN KEY (`superType_id`) REFERENCES `codes` (`id`),
  CONSTRAINT `FK_lv56tobhuavv8c9g4vw40ifdg` FOREIGN KEY (`subType_id`) REFERENCES `codes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_transitive_rels_attributes` (
  `code_transitive_rels_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_sg3oepd297j3uuqqsuw776sak` (`attributes_id`),
  KEY `FK_gpj3980afj9k550y2obnyepx` (`code_transitive_rels_id`),
  CONSTRAINT `FK_gpj3980afj9k550y2obnyepx` FOREIGN KEY (`code_transitive_rels_id`) REFERENCES `code_transitive_rels` (`id`),
  CONSTRAINT `FK_sg3oepd297j3uuqqsuw776sak` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_tree_positions` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ancestorPath` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `childCt` int NOT NULL,
  `descendantCt` int NOT NULL,
  `node_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_1v7ekguel39st32kiy2u12w9g` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_lsix0447i0htx2cadsr0pbp3b` (`node_id`),
  KEY `x_cdtr_t` (`terminology`),
  CONSTRAINT `FK_lsix0447i0htx2cadsr0pbp3b` FOREIGN KEY (`node_id`) REFERENCES `codes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_tree_positions_attributes` (
  `code_tree_positions_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_eje4qxh6imkyw75spgq3d7tpv` (`attributes_id`),
  KEY `FK_t9mw2ia3tfjogph6wpgfk1sng` (`code_tree_positions_id`),
  CONSTRAINT `FK_eje4qxh6imkyw75spgq3d7tpv` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_t9mw2ia3tfjogph6wpgfk1sng` FOREIGN KEY (`code_tree_positions_id`) REFERENCES `code_tree_positions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `codejpa_labels` (
  `CodeJpa_id` bigint NOT NULL,
  `labels` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_f2g1laj1lsvhfel86cg9rbj44` (`CodeJpa_id`),
  CONSTRAINT `FK_f2g1laj1lsvhfel86cg9rbj44` FOREIGN KEY (`CodeJpa_id`) REFERENCES `codes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coderelationshipjpa_alternateterminologyids` (
  `CodeRelationshipJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`CodeRelationshipJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_mlys34b5et00sd8nmhwirklex` FOREIGN KEY (`CodeRelationshipJpa_id`) REFERENCES `code_relationships` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `codes` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branchedTo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `name` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_lirxmb8jhswg19k3boli1115l` (`terminologyId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK_ff1t6cti9ehdukfx0oyllw0mj` (`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `codes_atoms` (
  `codes_id` bigint NOT NULL,
  `atoms_id` bigint NOT NULL,
  KEY `FK_98dw8th8xwa32mwne34jyy1wn` (`atoms_id`),
  KEY `FK_i76h7lwhahh0d0nkj4ubcko4d` (`codes_id`),
  CONSTRAINT `FK_98dw8th8xwa32mwne34jyy1wn` FOREIGN KEY (`atoms_id`) REFERENCES `atoms` (`id`),
  CONSTRAINT `FK_i76h7lwhahh0d0nkj4ubcko4d` FOREIGN KEY (`codes_id`) REFERENCES `codes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `codes_attributes` (
  `codes_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_7h5lkri1ggy08125636g02k5u` (`attributes_id`),
  KEY `FK_folhr6l5hk9n7cdd41jnywvvl` (`codes_id`),
  CONSTRAINT `FK_7h5lkri1ggy08125636g02k5u` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_folhr6l5hk9n7cdd41jnywvvl` FOREIGN KEY (`codes_id`) REFERENCES `codes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `component_histories` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `associatedRelease` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `reason` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `referencedTerminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `relationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_6ikufn4qkosopce7ql4ykqxc1` (`terminologyId`,`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `component_ids` (
  `TrackingRecordJpa_id` bigint NOT NULL,
  `componentIds` bigint DEFAULT NULL,
  KEY `FK_hnjpt8tvgln655nl1m8i8s08j` (`TrackingRecordJpa_id`),
  CONSTRAINT `FK_hnjpt8tvgln655nl1m8i8s08j` FOREIGN KEY (`TrackingRecordJpa_id`) REFERENCES `tracking_records` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `component_info_relationships` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `assertedDirection` bit(1) NOT NULL,
  `relGroup` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `hierarchical` bit(1) NOT NULL,
  `inferred` bit(1) NOT NULL,
  `relationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `stated` bit(1) NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `fromName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `fromTerminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `fromTerminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `fromType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `fromVersion` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `toName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `toTerminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `toTerminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `toType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `toVersion` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_mb49qr7s4modcgthbjvneddaj` (`terminologyId`,`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `component_info_relationships_attributes` (
  `component_info_relationships_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_9xa6qrkv4k6eshu347n7p8lwy` (`attributes_id`),
  KEY `FK_as2x7bscgvai08wmbequmdnfu` (`component_info_relationships_id`),
  CONSTRAINT `FK_9xa6qrkv4k6eshu347n7p8lwy` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_as2x7bscgvai08wmbequmdnfu` FOREIGN KEY (`component_info_relationships_id`) REFERENCES `component_info_relationships` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `componentinforelationshipjpa_alternateterminologyids` (
  `ComponentInfoRelationshipJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`ComponentInfoRelationshipJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_ekff3c8jhguurbamcu5vx6t5q` FOREIGN KEY (`ComponentInfoRelationshipJpa_id`) REFERENCES `component_info_relationships` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_notes` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `note` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `concept_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_lsy7pxwvdwk0qoy06yv2jmhkj` (`concept_id`),
  CONSTRAINT `FK_lsy7pxwvdwk0qoy06yv2jmhkj` FOREIGN KEY (`concept_id`) REFERENCES `concepts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_relationships` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `assertedDirection` bit(1) NOT NULL,
  `relGroup` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `hierarchical` bit(1) NOT NULL,
  `inferred` bit(1) NOT NULL,
  `relationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `stated` bit(1) NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `from_id` bigint NOT NULL,
  `to_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_qio5d2o01h8r14v5xintdj4fk` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_doytiony9gxdbjen9h80rpf0q` (`from_id`),
  KEY `FK_pjjs8bcjkw22bd7lhnguc7uir` (`to_id`),
  KEY `x_concept_rels_1` (`workflowStatus`),
  KEY `x_cr_t` (`terminology`),
  CONSTRAINT `FK_doytiony9gxdbjen9h80rpf0q` FOREIGN KEY (`from_id`) REFERENCES `concepts` (`id`),
  CONSTRAINT `FK_pjjs8bcjkw22bd7lhnguc7uir` FOREIGN KEY (`to_id`) REFERENCES `concepts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_relationships_attributes` (
  `concept_relationships_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_87twsing8pvkdmv7k76pxywlv` (`attributes_id`),
  KEY `FK_a7ybx732d6yubfx70adnh7djl` (`concept_relationships_id`),
  CONSTRAINT `FK_87twsing8pvkdmv7k76pxywlv` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_a7ybx732d6yubfx70adnh7djl` FOREIGN KEY (`concept_relationships_id`) REFERENCES `concept_relationships` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_subset_members` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `concept_id` bigint NOT NULL,
  `subset_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ixp98tkeenuaiswseua76ivob` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_evtov7haixf4ye5mydvranpmu` (`concept_id`),
  KEY `FK_yct8wbna2jvn3qk2k0nk0juo` (`subset_id`),
  KEY `x_csm_1` (`terminologyId`),
  CONSTRAINT `FK_evtov7haixf4ye5mydvranpmu` FOREIGN KEY (`concept_id`) REFERENCES `concepts` (`id`),
  CONSTRAINT `FK_yct8wbna2jvn3qk2k0nk0juo` FOREIGN KEY (`subset_id`) REFERENCES `concept_subsets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_subset_members_attributes` (
  `concept_subset_members_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_bgh9343lo8mh5pyv9fdypg4n` (`attributes_id`),
  KEY `FK_fmwn3m0onea2bl6n9cg35s6mb` (`concept_subset_members_id`),
  CONSTRAINT `FK_bgh9343lo8mh5pyv9fdypg4n` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_fmwn3m0onea2bl6n9cg35s6mb` FOREIGN KEY (`concept_subset_members_id`) REFERENCES `concept_subset_members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_subsets` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branchedTo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `description` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `disjointSubset` bit(1) NOT NULL,
  `labelSubset` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_2bcbxi2lv1xpmohacexc63mah` (`terminologyId`,`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_subsets_attributes` (
  `concept_subsets_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_n4qqe7dtwbiq54k5riq52qurd` (`attributes_id`),
  KEY `FK_20x6y6e4eu0vtxueb2d39ljcj` (`concept_subsets_id`),
  CONSTRAINT `FK_20x6y6e4eu0vtxueb2d39ljcj` FOREIGN KEY (`concept_subsets_id`) REFERENCES `concept_subsets` (`id`),
  CONSTRAINT `FK_n4qqe7dtwbiq54k5riq52qurd` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_transitive_rels` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `depth` int NOT NULL,
  `subType_id` bigint NOT NULL,
  `superType_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_auj6uuyp7980ajv20qdyw7fqn` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_f0u038kr0ytj1u5el3nk74wr2` (`subType_id`),
  KEY `FK_au2xnij8iqfxt8n597n65rd5m` (`superType_id`),
  CONSTRAINT `FK_au2xnij8iqfxt8n597n65rd5m` FOREIGN KEY (`superType_id`) REFERENCES `concepts` (`id`),
  CONSTRAINT `FK_f0u038kr0ytj1u5el3nk74wr2` FOREIGN KEY (`subType_id`) REFERENCES `concepts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_transitive_rels_attributes` (
  `concept_transitive_rels_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_rcnurepk41g7gi1q11m6qe6v9` (`attributes_id`),
  KEY `FK_6mw114356h0yq593twghq9xnx` (`concept_transitive_rels_id`),
  CONSTRAINT `FK_6mw114356h0yq593twghq9xnx` FOREIGN KEY (`concept_transitive_rels_id`) REFERENCES `concept_transitive_rels` (`id`),
  CONSTRAINT `FK_rcnurepk41g7gi1q11m6qe6v9` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_tree_positions` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ancestorPath` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `childCt` int NOT NULL,
  `descendantCt` int NOT NULL,
  `node_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_7xapju29pu9cw7cchor6n0o4e` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_lpg6b77ktpf6shvpf2yas870q` (`node_id`),
  KEY `x_ctr_t` (`terminology`),
  CONSTRAINT `FK_lpg6b77ktpf6shvpf2yas870q` FOREIGN KEY (`node_id`) REFERENCES `concepts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concept_tree_positions_attributes` (
  `concept_tree_positions_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_ll02onb4c6meh8hw138xl64cp` (`attributes_id`),
  KEY `FK_1maryfsldjy5qvxraxsw7clpt` (`concept_tree_positions_id`),
  CONSTRAINT `FK_1maryfsldjy5qvxraxsw7clpt` FOREIGN KEY (`concept_tree_positions_id`) REFERENCES `concept_tree_positions` (`id`),
  CONSTRAINT `FK_ll02onb4c6meh8hw138xl64cp` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conceptjpa_labels` (
  `ConceptJpa_id` bigint NOT NULL,
  `labels` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_7txudxjjv9ttu2h6sj8g39peh` (`ConceptJpa_id`),
  CONSTRAINT `FK_7txudxjjv9ttu2h6sj8g39peh` FOREIGN KEY (`ConceptJpa_id`) REFERENCES `concepts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conceptrelationshipjpa_alternateterminologyids` (
  `ConceptRelationshipJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`ConceptRelationshipJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_law1dv8h43xly2y02a8tlkrqe` FOREIGN KEY (`ConceptRelationshipJpa_id`) REFERENCES `concept_relationships` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concepts` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branchedTo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `name` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `anonymous` bit(1) NOT NULL,
  `fullyDefined` bit(1) NOT NULL,
  `lastApproved` datetime DEFAULT NULL,
  `lastApprovedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `usesRelationshipIntersection` bit(1) NOT NULL,
  `usesRelationshipUnion` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ckw6i7tv88dhgq45l2p22tghp` (`terminologyId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK_lstfpngk6t1g7xauu3remi6e2` (`terminology`,`version`,`id`),
  KEY `x_concepts_1` (`terminology`),
  KEY `x_concepts_2` (`workflowStatus`),
  KEY `idx_concepts_covering` (`terminology`,`name`(255),`publishable`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concepts_atoms` (
  `concepts_id` bigint NOT NULL,
  `atoms_id` bigint NOT NULL,
  KEY `FK_n2pihy61fmcek59e345iewcao` (`atoms_id`),
  KEY `FK_4yjnqn13rijkboyj4uj05lsnv` (`concepts_id`),
  CONSTRAINT `FK_4yjnqn13rijkboyj4uj05lsnv` FOREIGN KEY (`concepts_id`) REFERENCES `concepts` (`id`),
  CONSTRAINT `FK_n2pihy61fmcek59e345iewcao` FOREIGN KEY (`atoms_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concepts_attributes` (
  `concepts_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_oovev82w98adik3je2wckfhrx` (`attributes_id`),
  KEY `FK_49kw8evnbnxu1sl432qt2qlbj` (`concepts_id`),
  CONSTRAINT `FK_49kw8evnbnxu1sl432qt2qlbj` FOREIGN KEY (`concepts_id`) REFERENCES `concepts` (`id`),
  CONSTRAINT `FK_oovev82w98adik3je2wckfhrx` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concepts_component_histories` (
  `concepts_id` bigint NOT NULL,
  `componentHistories_id` bigint NOT NULL,
  UNIQUE KEY `UK_gdsxrgmjso4b43j16akqf3o04` (`componentHistories_id`),
  KEY `FK_lfq9bx0bti0n3g585sxqmmynu` (`concepts_id`),
  CONSTRAINT `FK_gdsxrgmjso4b43j16akqf3o04` FOREIGN KEY (`componentHistories_id`) REFERENCES `component_histories` (`id`),
  CONSTRAINT `FK_lfq9bx0bti0n3g585sxqmmynu` FOREIGN KEY (`concepts_id`) REFERENCES `concepts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concepts_definitions` (
  `concepts_id` bigint NOT NULL,
  `definitions_id` bigint NOT NULL,
  UNIQUE KEY `UK_r9px71sc64w005u5j6a9i3q2o` (`definitions_id`),
  KEY `FK_bvocehi4qhhieqn32lsp5aet9` (`concepts_id`),
  CONSTRAINT `FK_bvocehi4qhhieqn32lsp5aet9` FOREIGN KEY (`concepts_id`) REFERENCES `concepts` (`id`),
  CONSTRAINT `FK_r9px71sc64w005u5j6a9i3q2o` FOREIGN KEY (`definitions_id`) REFERENCES `definitions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `concepts_semantic_type_components` (
  `concepts_id` bigint NOT NULL,
  `semanticTypes_id` bigint NOT NULL,
  UNIQUE KEY `UK_ktv8v6y5d0486of6fejclcaeb` (`semanticTypes_id`),
  KEY `FK_2prq0r6j6wyp4e9ttpfd15iu2` (`concepts_id`),
  CONSTRAINT `FK_2prq0r6j6wyp4e9ttpfd15iu2` FOREIGN KEY (`concepts_id`) REFERENCES `concepts` (`id`),
  CONSTRAINT `FK_ktv8v6y5d0486of6fejclcaeb` FOREIGN KEY (`semanticTypes_id`) REFERENCES `semantic_type_components` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conceptsubsetjpa_alternateterminologyids` (
  `ConceptSubsetJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`ConceptSubsetJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_4lj03wvqhtc7onllq9gtg1hnq` FOREIGN KEY (`ConceptSubsetJpa_id`) REFERENCES `concept_subsets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contact_info` (
  `id` bigint NOT NULL,
  `address1` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `address2` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `city` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `country` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `fax` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `organization` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `stateOrProvince` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `telephone` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `value` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `zipCode` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deep_atom_relationships` (
  `relationship_id` bigint DEFAULT NULL,
  `component_type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `relationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) DEFAULT NULL,
  `suppressible` bit(1) DEFAULT NULL,
  `published` bit(1) DEFAULT NULL,
  `publishable` bit(1) DEFAULT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastModifiedby` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastModified` datetime DEFAULT NULL,
  `from_atoms_id` bigint DEFAULT NULL,
  `to_atoms_id` bigint DEFAULT NULL,
  KEY `x_from_id` (`from_atoms_id`),
  KEY `x_to_id` (`to_atoms_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deep_concept_relationships` (
  `relationship_id` bigint DEFAULT NULL,
  `component_type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `relationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) DEFAULT NULL,
  `suppressible` bit(1) DEFAULT NULL,
  `published` bit(1) DEFAULT NULL,
  `publishable` bit(1) DEFAULT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastModifiedby` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastModified` datetime DEFAULT NULL,
  `from_concepts_id` bigint DEFAULT NULL,
  `to_concepts_id` bigint DEFAULT NULL,
  KEY `x_from_id` (`from_concepts_id`),
  KEY `x_to_id` (`to_concepts_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `definitionjpa_alternateterminologyids` (
  `DefinitionJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`DefinitionJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_ntw2khhxgsr3o6qss8dt9vnxm` FOREIGN KEY (`DefinitionJpa_id`) REFERENCES `definitions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `definitions` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `value` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_bxb1pj5jykhntx36u73td4ohd` (`terminologyId`,`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `definitions_attributes` (
  `definitions_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_8kycyerkd74niiq04uvukwfc` (`attributes_id`),
  KEY `FK_5kavqtbn9nbw0u1gdg9b5fmwq` (`definitions_id`),
  CONSTRAINT `FK_5kavqtbn9nbw0u1gdg9b5fmwq` FOREIGN KEY (`definitions_id`) REFERENCES `definitions` (`id`),
  CONSTRAINT `FK_8kycyerkd74niiq04uvukwfc` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptor_notes` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `note` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `descriptor_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_jobcli6mkpay295h8iiyryg27` (`descriptor_id`),
  CONSTRAINT `FK_jobcli6mkpay295h8iiyryg27` FOREIGN KEY (`descriptor_id`) REFERENCES `descriptors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptor_relationships` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `assertedDirection` bit(1) NOT NULL,
  `relGroup` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `hierarchical` bit(1) NOT NULL,
  `inferred` bit(1) NOT NULL,
  `relationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `stated` bit(1) NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `from_id` bigint NOT NULL,
  `to_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_j1w6nq18mhddtj5mhxib87iev` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_46nwaodul6f9fcx0laa0dqn2b` (`from_id`),
  KEY `FK_3j07ib4epjbf45rrda55cmvvo` (`to_id`),
  KEY `x_dr_t` (`terminology`),
  CONSTRAINT `FK_3j07ib4epjbf45rrda55cmvvo` FOREIGN KEY (`to_id`) REFERENCES `descriptors` (`id`),
  CONSTRAINT `FK_46nwaodul6f9fcx0laa0dqn2b` FOREIGN KEY (`from_id`) REFERENCES `descriptors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptor_relationships_attributes` (
  `descriptor_relationships_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_dsgcomjqhg448qdshvnnaj1xb` (`attributes_id`),
  KEY `FK_qb9o2ekqrml0fnl8bboju0vep` (`descriptor_relationships_id`),
  CONSTRAINT `FK_dsgcomjqhg448qdshvnnaj1xb` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_qb9o2ekqrml0fnl8bboju0vep` FOREIGN KEY (`descriptor_relationships_id`) REFERENCES `descriptor_relationships` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptor_transitive_rels` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `depth` int NOT NULL,
  `subType_id` bigint NOT NULL,
  `superType_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_fkoyuvsecoluw56o8n86uuka5` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_16ty7bvvqofhjc2nms13ynv97` (`subType_id`),
  KEY `FK_3fnv6jqdaxq8qce9nt2mqr4r5` (`superType_id`),
  CONSTRAINT `FK_16ty7bvvqofhjc2nms13ynv97` FOREIGN KEY (`subType_id`) REFERENCES `descriptors` (`id`),
  CONSTRAINT `FK_3fnv6jqdaxq8qce9nt2mqr4r5` FOREIGN KEY (`superType_id`) REFERENCES `descriptors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptor_transitive_rels_attributes` (
  `descriptor_transitive_rels_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_g1fev1g2k94r6jpwlxw3w7xep` (`attributes_id`),
  KEY `FK_d6q3tfxewldayx2mi0tss6rko` (`descriptor_transitive_rels_id`),
  CONSTRAINT `FK_d6q3tfxewldayx2mi0tss6rko` FOREIGN KEY (`descriptor_transitive_rels_id`) REFERENCES `descriptor_transitive_rels` (`id`),
  CONSTRAINT `FK_g1fev1g2k94r6jpwlxw3w7xep` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptor_tree_positions` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ancestorPath` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `childCt` int NOT NULL,
  `descendantCt` int NOT NULL,
  `node_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ed6ryc410w3d2ep72vbu6beny` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_rg4w2xwo2fj77kuckrk5fluhe` (`node_id`),
  KEY `x_dtr_t` (`terminology`),
  CONSTRAINT `FK_rg4w2xwo2fj77kuckrk5fluhe` FOREIGN KEY (`node_id`) REFERENCES `descriptors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptor_tree_positions_attributes` (
  `descriptor_tree_positions_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_hut8l1pn1e8t8cds82ji5nkwv` (`attributes_id`),
  KEY `FK_mqd9jt48tv0151w3arh3c6l44` (`descriptor_tree_positions_id`),
  CONSTRAINT `FK_hut8l1pn1e8t8cds82ji5nkwv` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_mqd9jt48tv0151w3arh3c6l44` FOREIGN KEY (`descriptor_tree_positions_id`) REFERENCES `descriptor_tree_positions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptorjpa_labels` (
  `DescriptorJpa_id` bigint NOT NULL,
  `labels` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_t5iiwvshr786y1hrnm13r5ggi` (`DescriptorJpa_id`),
  CONSTRAINT `FK_t5iiwvshr786y1hrnm13r5ggi` FOREIGN KEY (`DescriptorJpa_id`) REFERENCES `descriptors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptorrelationshipjpa_alternateterminologyids` (
  `DescriptorRelationshipJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`DescriptorRelationshipJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_4gfx781l1tfclf3qed5d1jk7e` FOREIGN KEY (`DescriptorRelationshipJpa_id`) REFERENCES `descriptor_relationships` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptors` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branchedTo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `name` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_peackdf122fhplwqvsksjknox` (`terminologyId`,`terminology`,`version`,`id`),
  UNIQUE KEY `UK_j2p373bohn0ox6jr27pdoscax` (`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptors_atoms` (
  `descriptors_id` bigint NOT NULL,
  `atoms_id` bigint NOT NULL,
  KEY `FK_ay1fslk8i4yrnl7ahe0u9gsa3` (`atoms_id`),
  KEY `FK_hubrpps8u1n6d2kbqbcjq12yi` (`descriptors_id`),
  CONSTRAINT `FK_ay1fslk8i4yrnl7ahe0u9gsa3` FOREIGN KEY (`atoms_id`) REFERENCES `atoms` (`id`),
  CONSTRAINT `FK_hubrpps8u1n6d2kbqbcjq12yi` FOREIGN KEY (`descriptors_id`) REFERENCES `descriptors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptors_attributes` (
  `descriptors_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_qi1r52ewa7homr8d3r5qyrhnt` (`attributes_id`),
  KEY `FK_1t6ghk88if6ivwi93rqygj37q` (`descriptors_id`),
  CONSTRAINT `FK_1t6ghk88if6ivwi93rqygj37q` FOREIGN KEY (`descriptors_id`) REFERENCES `descriptors` (`id`),
  CONSTRAINT `FK_qi1r52ewa7homr8d3r5qyrhnt` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `descriptors_definitions` (
  `descriptors_id` bigint NOT NULL,
  `definitions_id` bigint NOT NULL,
  UNIQUE KEY `UK_6y1os5fsok7tn2t4mbjp1dtuj` (`definitions_id`),
  KEY `FK_2s9yoo1rnd1p3mi96yg9r2lwl` (`descriptors_id`),
  CONSTRAINT `FK_2s9yoo1rnd1p3mi96yg9r2lwl` FOREIGN KEY (`descriptors_id`) REFERENCES `descriptors` (`id`),
  CONSTRAINT `FK_6y1os5fsok7tn2t4mbjp1dtuj` FOREIGN KEY (`definitions_id`) REFERENCES `definitions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `general_concept_axioms` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `equivalent` bit(1) NOT NULL,
  `subClass` bit(1) NOT NULL,
  `leftHandSide_id` bigint NOT NULL,
  `rightHandSide_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_4dimr4pgl20sg2g9ox9dej434` (`terminologyId`,`terminology`,`version`,`id`),
  KEY `FK_h2lt0cllg75i7rvallc267vci` (`leftHandSide_id`),
  KEY `FK_8uj38kbg4g5d36gfmwo59tiy0` (`rightHandSide_id`),
  CONSTRAINT `FK_8uj38kbg4g5d36gfmwo59tiy0` FOREIGN KEY (`rightHandSide_id`) REFERENCES `concepts` (`id`),
  CONSTRAINT `FK_h2lt0cllg75i7rvallc267vci` FOREIGN KEY (`leftHandSide_id`) REFERENCES `concepts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `general_metadata_entries` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `abbreviation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `expandedForm` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `metadataKey` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `keyType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hibernate_sequence` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hibernate_sequences` (
  `sequence_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `sequence_next_hi_value` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `label_sets` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `abbreviation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `expandedForm` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `derived` bit(1) NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_l41gste8ceh10cq76duo0bt5i` (`abbreviation`,`terminology`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `languages` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `abbreviation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `expandedForm` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `iso3Code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `isoCode` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_m6dwko4yeneeabbca9at7h6kj` (`abbreviation`,`terminology`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lexical_class_identity` (
  `id` bigint NOT NULL,
  `language` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `normalizedName` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `normalizedNameHash` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_etuxedtellivpx9uy03y50xle` (`normalizedNameHash`,`language`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lexical_classes` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branchedTo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `name` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `language` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `normalizedName` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_sirhwp2981v1gq6whqtam85cg` (`terminologyId`,`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lexical_classes_atoms` (
  `lexical_classes_id` bigint NOT NULL,
  `atoms_id` bigint NOT NULL,
  KEY `FK_6hc0memnnbmdbdiwt2kt7xw8` (`atoms_id`),
  KEY `FK_6f143i6mnqijwr1ngsm12geup` (`lexical_classes_id`),
  CONSTRAINT `FK_6f143i6mnqijwr1ngsm12geup` FOREIGN KEY (`lexical_classes_id`) REFERENCES `lexical_classes` (`id`),
  CONSTRAINT `FK_6hc0memnnbmdbdiwt2kt7xw8` FOREIGN KEY (`atoms_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lexical_classes_attributes` (
  `lexical_classes_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_se2jw5t0vuvxvpp4qwu4a5n9e` (`attributes_id`),
  KEY `FK_q3svlcd5ommkbjw4t32rnaywr` (`lexical_classes_id`),
  CONSTRAINT `FK_q3svlcd5ommkbjw4t32rnaywr` FOREIGN KEY (`lexical_classes_id`) REFERENCES `lexical_classes` (`id`),
  CONSTRAINT `FK_se2jw5t0vuvxvpp4qwu4a5n9e` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lexicalclassjpa_labels` (
  `LexicalClassJpa_id` bigint NOT NULL,
  `labels` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_a69o49130nmskp5ptv64s278k` (`LexicalClassJpa_id`),
  CONSTRAINT `FK_a69o49130nmskp5ptv64s278k` FOREIGN KEY (`LexicalClassJpa_id`) REFERENCES `lexical_classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_entries` (
  `id` bigint NOT NULL,
  `activityId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `message` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `objectId` bigint DEFAULT NULL,
  `projectId` bigint DEFAULT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `timestamp` datetime NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `workId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mappingjpa_alternateterminologyids` (
  `MappingJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`MappingJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_k48k1ea10aryv26u9j5tkrpq8` FOREIGN KEY (`MappingJpa_id`) REFERENCES `mappings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mappings` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `advice` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `fromIdType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `fromName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `fromTerminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `mapGroup` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `rank` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `relationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `rule` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `toIdType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `toName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `toTerminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `mapSet_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_pimos785t8i8b9dexxqabmoqp` (`fromTerminologyId`,`toTerminologyId`,`terminology`,`version`,`id`),
  KEY `FK_tkcors1p6uycpmayok4j3fqwp` (`mapSet_id`),
  CONSTRAINT `FK_tkcors1p6uycpmayok4j3fqwp` FOREIGN KEY (`mapSet_id`) REFERENCES `mapsets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mappings_attributes` (
  `mappings_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_2ny13a2yu9vrt2dvrue64kv2x` (`attributes_id`),
  KEY `FK_qjrynvh4xgrsd1eljbavj2jxv` (`mappings_id`),
  CONSTRAINT `FK_2ny13a2yu9vrt2dvrue64kv2x` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_qjrynvh4xgrsd1eljbavj2jxv` FOREIGN KEY (`mappings_id`) REFERENCES `mappings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mapsetjpa_alternateterminologyids` (
  `MapSetJpa_id` bigint NOT NULL,
  `alternateTerminologyIds` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `alternateTerminologyIds_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`MapSetJpa_id`,`alternateTerminologyIds_KEY`),
  CONSTRAINT `FK_boa9w5kcmuf094iiyqjjg6fcg` FOREIGN KEY (`MapSetJpa_id`) REFERENCES `mapsets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mapsets` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `complexity` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `fromComplexity` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `fromExhaustive` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `fromTerminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `fromVersion` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `toComplexity` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `toExhaustive` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `toTerminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `toVersion` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_oflpwchj5bfkxqvadhj46wjxx` (`terminologyId`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mapsets_attributes` (
  `mapsets_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_a1tfp35h17fsbdl07p9xeex2h` (`attributes_id`),
  KEY `FK_3udurc8qqdddhboq9u6jtvdk8` (`mapsets_id`),
  CONSTRAINT `FK_3udurc8qqdddhboq9u6jtvdk8` FOREIGN KEY (`mapsets_id`) REFERENCES `mapsets` (`id`),
  CONSTRAINT `FK_a1tfp35h17fsbdl07p9xeex2h` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `molecular_actions` (
  `id` bigint NOT NULL,
  `activityId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `batchId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `componentId` bigint NOT NULL,
  `componentId2` bigint DEFAULT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `macroAction` bit(1) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `undoneFlag` bit(1) NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `workId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mrcui` (
  `cui1` varchar(255) DEFAULT NULL,
  `version` varchar(255) DEFAULT NULL,
  `rel` varchar(255) DEFAULT NULL,
  `rela` varchar(255) DEFAULT NULL,
  `mapreason` varchar(255) DEFAULT NULL,
  `cui2` varchar(255) DEFAULT NULL,
  `mapin` bit(1) DEFAULT NULL,
  KEY `cui1` (`cui1`),
  KEY `idx_mrcui_cui1_rel` (`cui1`,`rel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orig_concept_ids` (
  `TrackingRecordJpa_id` bigint NOT NULL,
  `origConceptIds` bigint DEFAULT NULL,
  KEY `FK_1pp417erv11dg367elqop2nbt` (`TrackingRecordJpa_id`),
  CONSTRAINT `FK_1pp417erv11dg367elqop2nbt` FOREIGN KEY (`TrackingRecordJpa_id`) REFERENCES `tracking_records` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `precedence_list_term_types` (
  `PrecedenceListJpa_id` bigint NOT NULL,
  `termTypes` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `termTypes_ORDER` int NOT NULL,
  PRIMARY KEY (`PrecedenceListJpa_id`,`termTypes_ORDER`),
  CONSTRAINT `FK_hox56rdf324xgjohcr805qm9t` FOREIGN KEY (`PrecedenceListJpa_id`) REFERENCES `precedence_lists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `precedence_list_terminologies` (
  `PrecedenceListJpa_id` bigint NOT NULL,
  `terminologies` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `terminologies_ORDER` int NOT NULL,
  PRIMARY KEY (`PrecedenceListJpa_id`,`terminologies_ORDER`),
  CONSTRAINT `FK_9vsy8acsbjglks0t52ul30iik` FOREIGN KEY (`PrecedenceListJpa_id`) REFERENCES `precedence_lists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `precedence_lists` (
  `id` bigint NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `process_configs` (
  `id` bigint NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `feedbackEmail` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `inputPath` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `project_id` bigint NOT NULL,
  `logPath` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_4bwrewltedooq28q9703tgr8s` (`name`,`project_id`),
  KEY `FK_aee37v7cumd2x95xuq14sehmm` (`project_id`),
  CONSTRAINT `FK_aee37v7cumd2x95xuq14sehmm` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `process_executions` (
  `id` bigint NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `feedbackEmail` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `failDate` datetime DEFAULT NULL,
  `finishDate` datetime DEFAULT NULL,
  `inputPath` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `processConfigId` bigint NOT NULL,
  `startDate` datetime DEFAULT NULL,
  `stopDate` datetime DEFAULT NULL,
  `type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `warning` bit(1) NOT NULL,
  `workId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `project_id` bigint NOT NULL,
  `logPath` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_1k6b4ht5o65h4vyb89hci2grc` (`project_id`),
  CONSTRAINT `FK_1k6b4ht5o65h4vyb89hci2grc` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `processexecutionjpa_executioninfo` (
  `ProcessExecutionJpa_id` bigint NOT NULL,
  `executionInfo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `executionInfo_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`ProcessExecutionJpa_id`,`executionInfo_KEY`),
  CONSTRAINT `FK_rkgoaxmxy1wk746a63dlc8ieo` FOREIGN KEY (`ProcessExecutionJpa_id`) REFERENCES `process_executions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_user_role_map` (
  `ProjectJpa_id` bigint NOT NULL,
  `role` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`ProjectJpa_id`,`user_id`),
  KEY `FK_g5m2u4br4321soo2ntmuy3cgi` (`user_id`),
  CONSTRAINT `FK_g5m2u4br4321soo2ntmuy3cgi` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK_t0etiaklg9wipjs1bqy5bnm2p` FOREIGN KEY (`ProjectJpa_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_validation_checks` (
  `ProjectJpa_id` bigint NOT NULL,
  `validationChecks` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_2x3y88gwetdlibgormw3m66q` (`ProjectJpa_id`),
  CONSTRAINT `FK_2x3y88gwetdlibgormw3m66q` FOREIGN KEY (`ProjectJpa_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `projectjpa_newatomtermgroups` (
  `ProjectJpa_id` bigint NOT NULL,
  `newAtomTermgroups` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_5hassyddikjkymow9hpui8ra6` (`ProjectJpa_id`),
  CONSTRAINT `FK_5hassyddikjkymow9hpui8ra6` FOREIGN KEY (`ProjectJpa_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `projectjpa_semantictypecategorymap` (
  `ProjectJpa_id` bigint NOT NULL,
  `semanticTypeCategoryMap` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `semanticTypeCategoryMap_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`ProjectJpa_id`,`semanticTypeCategoryMap_KEY`),
  CONSTRAINT `FK_o76d1ejsfkuw1fs5f4f1cmucw` FOREIGN KEY (`ProjectJpa_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `projects` (
  `id` bigint NOT NULL,
  `automationsEnabled` bit(1) NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `editingEnabled` bit(1) NOT NULL,
  `feedbackEmail` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `isPublic` bit(1) NOT NULL,
  `language` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `organization` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `teamBased` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `workflowPath` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `precedenceList_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_929fa48vu87c3mhmcu7jwmxdw` (`name`,`description`),
  KEY `FK_8tyrmneae4du82xatc9fomacy` (`precedenceList_id`),
  CONSTRAINT `FK_8tyrmneae4du82xatc9fomacy` FOREIGN KEY (`precedenceList_id`) REFERENCES `precedence_lists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `projects_type_key_values` (
  `projects_id` bigint NOT NULL,
  `validationData_id` bigint NOT NULL,
  UNIQUE KEY `UK_obpctxv9dtwtw8julhppedwcr` (`validationData_id`),
  KEY `FK_h68fmypqfk15h00asu292dsv` (`projects_id`),
  CONSTRAINT `FK_h68fmypqfk15h00asu292dsv` FOREIGN KEY (`projects_id`) REFERENCES `projects` (`id`),
  CONSTRAINT `FK_obpctxv9dtwtw8julhppedwcr` FOREIGN KEY (`validationData_id`) REFERENCES `type_key_values` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `property_chains` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `abbreviation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `expandedForm` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `result_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_1nuktbrp6v392km28bupfmt9r` (`abbreviation`,`terminology`),
  KEY `FK_3urnef5kb465cq1q1i2je9793` (`result_id`),
  CONSTRAINT `FK_3urnef5kb465cq1q1i2je9793` FOREIGN KEY (`result_id`) REFERENCES `additional_relationship_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `property_chains_additional_relationship_types` (
  `property_chains_id` bigint NOT NULL,
  `chain_id` bigint NOT NULL,
  `PropertyChainJpa_id` bigint NOT NULL,
  KEY `FK_kkuym3rpw0cany0fji39pb1un` (`chain_id`),
  KEY `FK_vvopnatcyhhxwy2i13fpmf4y` (`property_chains_id`),
  CONSTRAINT `FK_kkuym3rpw0cany0fji39pb1un` FOREIGN KEY (`chain_id`) REFERENCES `additional_relationship_types` (`id`),
  CONSTRAINT `FK_vvopnatcyhhxwy2i13fpmf4y` FOREIGN KEY (`property_chains_id`) REFERENCES `property_chains` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `relationship_identity` (
  `id` bigint NOT NULL,
  `additionalRelationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `fromId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `fromTerminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `fromType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `inverseId` bigint NOT NULL,
  `relationshipType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `toId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `toTerminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `toType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_fmcu1hj3t8hgudyuvkgr3pehq` (`fromId`,`fromTerminology`,`fromType`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `relationship_types` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `abbreviation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `expandedForm` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `hierarchical` bit(1) NOT NULL,
  `inverse_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_lanx8cetclav1tnvlmkj41v9x` (`abbreviation`,`terminology`),
  KEY `FK_8xrkl05ar5ovxf4tjv1u1yw3w` (`inverse_id`),
  CONSTRAINT `FK_8xrkl05ar5ovxf4tjv1u1yw3w` FOREIGN KEY (`inverse_id`) REFERENCES `relationship_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `release_infos` (
  `id` bigint NOT NULL,
  `description` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `effectiveTime` datetime DEFAULT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `planned` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `releaseBeginDate` datetime DEFAULT NULL,
  `releaseFinishDate` datetime DEFAULT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_75b8arbamsvcbcxspu2gl9n7f` (`name`,`terminology`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `release_infos_release_properties` (
  `release_infos_id` bigint NOT NULL,
  `properties_id` bigint NOT NULL,
  UNIQUE KEY `UK_esdlrqwg62ua7vy6lf8n2dhjy` (`properties_id`),
  KEY `FK_n0qv4w20j47y0pvirr42hhwv` (`release_infos_id`),
  CONSTRAINT `FK_esdlrqwg62ua7vy6lf8n2dhjy` FOREIGN KEY (`properties_id`) REFERENCES `release_properties` (`id`),
  CONSTRAINT `FK_n0qv4w20j47y0pvirr42hhwv` FOREIGN KEY (`release_infos_id`) REFERENCES `release_infos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `release_properties` (
  `id` bigint NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `value` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_result_items` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `itemId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `itemName` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `result_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_t0fdufvbn4796xwjw06ii7xfj` (`result_id`),
  CONSTRAINT `FK_t0fdufvbn4796xwjw06ii7xfj` FOREIGN KEY (`result_id`) REFERENCES `report_results` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_results` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `ct` bigint NOT NULL,
  `value` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `report_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_sve3cxkcaq7va4bly69puv7ga` (`report_id`),
  CONSTRAINT `FK_sve3cxkcaq7va4bly69puv7ga` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `autoGenerated` bit(1) NOT NULL,
  `diffReport` bit(1) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `query` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `queryType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `report1Id` bigint DEFAULT NULL,
  `report2Id` bigint DEFAULT NULL,
  `resultType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_7rikei6vwv558nnq4000ti23t` (`project_id`),
  CONSTRAINT `FK_7rikei6vwv558nnq4000ti23t` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `revinfo` (
  `REV` int NOT NULL AUTO_INCREMENT,
  `REVTSTMP` bigint DEFAULT NULL,
  PRIMARY KEY (`REV`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `root_terminologies` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `family` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `hierarchicalName` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `hierarchyComputable` bit(1) NOT NULL,
  `language` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `polyhierarchy` bit(1) NOT NULL,
  `preferredName` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `restrictionLevel` int NOT NULL,
  `shortName` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `acquisitionContact_id` bigint DEFAULT NULL,
  `contentContact_id` bigint DEFAULT NULL,
  `licenseContact_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_5aosox1h4gdk6pxcl455dpal2` (`terminology`),
  KEY `FK_c0l7hdkjpg3djdy3b8u9o2r8e` (`acquisitionContact_id`),
  KEY `FK_ndcs2647o6bggvoscx8tclm7h` (`contentContact_id`),
  KEY `FK_89bcugs55yn4i8qfqfhdgsufo` (`licenseContact_id`),
  CONSTRAINT `FK_89bcugs55yn4i8qfqfhdgsufo` FOREIGN KEY (`licenseContact_id`) REFERENCES `contact_info` (`id`),
  CONSTRAINT `FK_c0l7hdkjpg3djdy3b8u9o2r8e` FOREIGN KEY (`acquisitionContact_id`) REFERENCES `contact_info` (`id`),
  CONSTRAINT `FK_ndcs2647o6bggvoscx8tclm7h` FOREIGN KEY (`contentContact_id`) REFERENCES `contact_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rootterminologyjpa_synonymousnames` (
  `RootTerminologyJpa_id` bigint NOT NULL,
  `synonymousNames` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_i4td0ng3302gd3kkdkecwbxpm` (`RootTerminologyJpa_id`),
  CONSTRAINT `FK_i4td0ng3302gd3kkdkecwbxpm` FOREIGN KEY (`RootTerminologyJpa_id`) REFERENCES `root_terminologies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `ruis_m4` AS SELECT 
 1 AS `relationship_id`,
 1 AS `type`,
 1 AS `rui`*/;
SET character_set_client = @saved_cs_client;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semantic_type_components` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `semanticType` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_dm2b69066m3v1xpok49xsh5wp` (`terminologyId`,`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semantic_types` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `abbreviation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `expandedForm` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `definition` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `example` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `functionalChemical` bit(1) NOT NULL,
  `nonHuman` bit(1) NOT NULL,
  `structuralChemical` bit(1) NOT NULL,
  `treeNumber` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `typeId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `usageNote` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  PRIMARY KEY (`id`),
  KEY `semantic_types_idx_expandedform` (`expandedForm`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `source_data` (
  `id` bigint NOT NULL,
  `description` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `handler` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `handlerStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `releaseVersion` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `status` int DEFAULT NULL,
  `statusText` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `timestamp` datetime NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_cxjeelll3rt0xsmsq6ugs0vl7` (`name`),
  UNIQUE KEY `UK_3ixfta31n4gclaqth72ahcwoi` (`name`,`terminology`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `source_data_files` (
  `id` bigint NOT NULL,
  `directory` bit(1) NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(250) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(250) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `path` varchar(250) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `size` bigint NOT NULL,
  `timestamp` datetime NOT NULL,
  `sourceData_id` bigint DEFAULT NULL,
  `fileSize` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_iqhlphyy3883s2kkq80vgg1ts` (`path`,`name`,`directory`),
  UNIQUE KEY `UK_bn72jn1jow9veqsim3llrcttm` (`path`),
  KEY `FK_jq7crmjb98difsawoo5xmyk49` (`sourceData_id`),
  CONSTRAINT `FK_jq7crmjb98difsawoo5xmyk49` FOREIGN KEY (`sourceData_id`) REFERENCES `source_data` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `source_data_source_data_files` (
  `source_data_id` bigint NOT NULL,
  `sourceDataFiles_id` bigint NOT NULL,
  `SourceDataJpa_id` bigint NOT NULL,
  UNIQUE KEY `UK_h3kxoyr43p3q5yhlyj2l83bel` (`sourceDataFiles_id`),
  KEY `FK_p8dv61ofrkal6ome4cb20ov3w` (`source_data_id`),
  CONSTRAINT `FK_h3kxoyr43p3q5yhlyj2l83bel` FOREIGN KEY (`sourceDataFiles_id`) REFERENCES `source_data_files` (`id`),
  CONSTRAINT `FK_p8dv61ofrkal6ome4cb20ov3w` FOREIGN KEY (`source_data_id`) REFERENCES `source_data` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `source_id_ranges` (
  `id` bigint NOT NULL,
  `beginSourceId` bigint NOT NULL,
  `endSourceId` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) NOT NULL,
  `terminology` varchar(255) NOT NULL,
  `timestamp` datetime NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_28x5qdepvh23cvgdw8it46div` (`terminology`),
  KEY `FK_abs30fwlojw15t1c1ylr0h9or` (`project_id`),
  CONSTRAINT `FK_abs30fwlojw15t1c1ylr0h9or` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `string_class_identity` (
  `id` bigint NOT NULL,
  `language` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `nameHash` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_8ndwbp1k68nx1ji8qalcw9otb` (`nameHash`,`language`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `string_classes` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `obsolete` bit(1) NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `suppressible` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branchedTo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `name` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `language` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_oaue390239r0q5ntbl64br6jy` (`terminologyId`,`terminology`,`version`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `string_classes_atoms` (
  `string_classes_id` bigint NOT NULL,
  `atoms_id` bigint NOT NULL,
  KEY `FK_ryobb4fx88hk6vo0gayumot9d` (`atoms_id`),
  KEY `FK_4842fvi6tnn7gdeaj48esg7fr` (`string_classes_id`),
  CONSTRAINT `FK_4842fvi6tnn7gdeaj48esg7fr` FOREIGN KEY (`string_classes_id`) REFERENCES `string_classes` (`id`),
  CONSTRAINT `FK_ryobb4fx88hk6vo0gayumot9d` FOREIGN KEY (`atoms_id`) REFERENCES `atoms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `string_classes_attributes` (
  `string_classes_id` bigint NOT NULL,
  `attributes_id` bigint NOT NULL,
  UNIQUE KEY `UK_c3s929555tubcugsj75ustp3w` (`attributes_id`),
  KEY `FK_huh6qbwy4ogpu5jd94mkacjjv` (`string_classes_id`),
  CONSTRAINT `FK_c3s929555tubcugsj75ustp3w` FOREIGN KEY (`attributes_id`) REFERENCES `attributes` (`id`),
  CONSTRAINT `FK_huh6qbwy4ogpu5jd94mkacjjv` FOREIGN KEY (`string_classes_id`) REFERENCES `string_classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stringclassjpa_labels` (
  `StringClassJpa_id` bigint NOT NULL,
  `labels` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_1sueu459b1bif1fpcdvytv8vn` (`StringClassJpa_id`),
  CONSTRAINT `FK_1sueu459b1bif1fpcdvytv8vn` FOREIGN KEY (`StringClassJpa_id`) REFERENCES `string_classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sty_identity` (
  `id` bigint NOT NULL,
  `conceptTerminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `semanticType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_30vl37s8eii9nios3npefhigg` (`conceptTerminologyId`,`semanticType`,`terminology`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_generator` (
  `sequence_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `next_val` bigint DEFAULT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_generator_action` (
  `sequence_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `next_val` bigint DEFAULT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_generator_log` (
  `sequence_name` varchar(255) NOT NULL,
  `next_val` bigint DEFAULT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_generator_process` (
  `sequence_name` varchar(255) NOT NULL,
  `next_val` bigint DEFAULT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_generator_release` (
  `sequence_name` varchar(255) NOT NULL,
  `next_val` bigint DEFAULT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_generator_report` (
  `sequence_name` varchar(255) NOT NULL,
  `next_val` bigint DEFAULT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_generator_transformer` (
  `sequence_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `next_val` bigint DEFAULT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_generator_users` (
  `sequence_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `next_val` bigint DEFAULT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_generator_wf` (
  `sequence_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `next_val` bigint DEFAULT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `term_types` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `abbreviation` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `branch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `expandedForm` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `publishable` bit(1) NOT NULL,
  `published` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `codeVariantType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `exclude` bit(1) NOT NULL,
  `hierarchicalType` bit(1) NOT NULL,
  `nameVariantType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `normExclude` bit(1) NOT NULL,
  `obsolete` bit(1) NOT NULL,
  `style` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `suppressible` bit(1) NOT NULL,
  `usageType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ao3mrkos66f9g0cy12706glac` (`abbreviation`,`terminology`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terminologies` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `assertsRelDirection` bit(1) NOT NULL,
  `current` bit(1) NOT NULL,
  `descriptionLogicProfile` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `descriptionLogicTerminology` bit(1) NOT NULL,
  `endDate` datetime DEFAULT NULL,
  `includeSiblings` bit(1) NOT NULL,
  `inverterEmail` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `metathesaurus` bit(1) NOT NULL,
  `organizingClassType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `preferredName` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `startDate` datetime DEFAULT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `citation_id` bigint DEFAULT NULL,
  `rootTerminology_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_cyydg2sde04jdkepugn65pjwd` (`terminology`,`version`),
  KEY `FK_68acc8toe0e0brt7ucjusv3el` (`citation_id`),
  KEY `FK_tensavf81840en4erwq44v0jd` (`rootTerminology_id`),
  CONSTRAINT `FK_68acc8toe0e0brt7ucjusv3el` FOREIGN KEY (`citation_id`) REFERENCES `citations` (`id`),
  CONSTRAINT `FK_tensavf81840en4erwq44v0jd` FOREIGN KEY (`rootTerminology_id`) REFERENCES `root_terminologies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terminologyjpa_firstreleases` (
  `TerminologyJpa_id` bigint NOT NULL,
  `firstReleases` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `firstReleases_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`TerminologyJpa_id`,`firstReleases_KEY`),
  CONSTRAINT `FK_3g0ey21d74u98pbkcgjhos7kd` FOREIGN KEY (`TerminologyJpa_id`) REFERENCES `terminologies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terminologyjpa_lastreleases` (
  `TerminologyJpa_id` bigint NOT NULL,
  `lastReleases` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastReleases_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`TerminologyJpa_id`,`lastReleases_KEY`),
  CONSTRAINT `FK_7pl6muhdm1ef9rd7udxoa947e` FOREIGN KEY (`TerminologyJpa_id`) REFERENCES `terminologies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terminologyjpa_relatedterminologies` (
  `TerminologyJpa_id` bigint NOT NULL,
  `relatedTerminologies` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_qv0i7j8hka8bh54pdrwnw8693` (`TerminologyJpa_id`),
  CONSTRAINT `FK_qv0i7j8hka8bh54pdrwnw8693` FOREIGN KEY (`TerminologyJpa_id`) REFERENCES `terminologies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terminologyjpa_synonymousnames` (
  `TerminologyJpa_id` bigint NOT NULL,
  `synonymousNames` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_l5f6a587ncsukrtd4vk29ty3a` (`TerminologyJpa_id`),
  CONSTRAINT `FK_l5f6a587ncsukrtd4vk29ty3a` FOREIGN KEY (`TerminologyJpa_id`) REFERENCES `terminologies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tracking_records` (
  `id` bigint NOT NULL,
  `checklistName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `clusterId` bigint NOT NULL,
  `clusterType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `finished` bit(1) NOT NULL,
  `indexedData` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `workflowBinName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `worklistName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_i5upqxttker09yj3pbpsfadn7` (`project_id`),
  CONSTRAINT `FK_i5upqxttker09yj3pbpsfadn7` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `type_key_values` (
  `id` bigint NOT NULL,
  `key_field` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `value` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preferences` (
  `id` bigint NOT NULL,
  `feedbackEmail` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastProjectId` bigint DEFAULT NULL,
  `lastProjectRole` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastTab` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `lastTerminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `precedenceList_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_3v9nljfcb8cw4ab9hhwuum7qt` (`precedenceList_id`),
  KEY `FK_qy8dkrkc8b34dcgwoq2km43rd` (`user_id`),
  CONSTRAINT `FK_3v9nljfcb8cw4ab9hhwuum7qt` FOREIGN KEY (`precedenceList_id`) REFERENCES `precedence_lists` (`id`),
  CONSTRAINT `FK_qy8dkrkc8b34dcgwoq2km43rd` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_project_role_map` (
  `UserJpa_id` bigint NOT NULL,
  `role` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`UserJpa_id`,`project_id`),
  KEY `FK_280e3veus46tdmcmvw918tg4q` (`project_id`),
  CONSTRAINT `FK_280e3veus46tdmcmvw918tg4q` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`),
  CONSTRAINT `FK_7ppgoj8kxsmh27hyahk1m96v7` FOREIGN KEY (`UserJpa_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userpreferencesjpa_favorites` (
  `UserPreferencesJpa_id` bigint NOT NULL,
  `favorites` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_h8ki39ds3a2pi4is59lfwk33v` (`UserPreferencesJpa_id`),
  CONSTRAINT `FK_h8ki39ds3a2pi4is59lfwk33v` FOREIGN KEY (`UserPreferencesJpa_id`) REFERENCES `user_preferences` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userpreferencesjpa_properties` (
  `UserPreferencesJpa_id` bigint NOT NULL,
  `properties` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin,
  `properties_KEY` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`UserPreferencesJpa_id`,`properties_KEY`),
  CONSTRAINT `FK_d2fcqrtvkl7ltf1adtgvt3a94` FOREIGN KEY (`UserPreferencesJpa_id`) REFERENCES `user_preferences` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL,
  `applicationRole` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `editorLevel` int NOT NULL,
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `team` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `userName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_mmns67o5v4bfippoqitu4v3t6` (`userName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `valid_categories` (
  `ProjectJpa_id` bigint NOT NULL,
  `validCategories` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_mkfiruasjgc2wa7pytr3ea69g` (`ProjectJpa_id`),
  CONSTRAINT `FK_mkfiruasjgc2wa7pytr3ea69g` FOREIGN KEY (`ProjectJpa_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workflow_bin_definitions` (
  `id` bigint NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `editable` bit(1) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `query` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `queryType` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `required` bit(1) NOT NULL,
  `timestamp` datetime NOT NULL,
  `workflowConfig_id` bigint NOT NULL,
  `workflowBinDefinitions_ORDER` int DEFAULT NULL,
  `autofix` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_h0m5x5xiua1tf3sjvmnrepyp5` (`name`,`workflowConfig_id`),
  KEY `FK_2o7kiyqgqbygvfqj2l6gr45bc` (`workflowConfig_id`),
  CONSTRAINT `FK_2o7kiyqgqbygvfqj2l6gr45bc` FOREIGN KEY (`workflowConfig_id`) REFERENCES `workflow_configs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workflow_bins` (
  `id` bigint NOT NULL,
  `clusterCt` int NOT NULL,
  `creationTime` bigint NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `editable` bit(1) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `rank` int NOT NULL,
  `required` bit(1) NOT NULL,
  `terminology` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `terminologyId` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `version` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `project_id` bigint NOT NULL,
  `autofix` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_m44ihnvdav8clwmweal4fcjql` (`name`,`type`,`project_id`),
  KEY `FK_tfe22nn1hkax2tqcgwbaai8s1` (`project_id`),
  CONSTRAINT `FK_tfe22nn1hkax2tqcgwbaai8s1` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workflow_bins_tracking_records` (
  `workflow_bins_id` bigint NOT NULL,
  `trackingRecords_id` bigint NOT NULL,
  UNIQUE KEY `UK_9h94iwui43pv0pcj8vreqf0ck` (`trackingRecords_id`),
  KEY `FK_il77didlab40f1bnmvlf18kq1` (`workflow_bins_id`),
  CONSTRAINT `FK_9h94iwui43pv0pcj8vreqf0ck` FOREIGN KEY (`trackingRecords_id`) REFERENCES `tracking_records` (`id`),
  CONSTRAINT `FK_il77didlab40f1bnmvlf18kq1` FOREIGN KEY (`workflow_bins_id`) REFERENCES `workflow_bins` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workflow_configs` (
  `id` bigint NOT NULL,
  `adminConfig` bit(1) NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `lastPartitionTime` bigint DEFAULT NULL,
  `mutuallyExclusive` bit(1) NOT NULL,
  `queryStyle` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_iwxk18eadh8v8dwe0uhjts0gl` (`project_id`,`type`),
  CONSTRAINT `FK_8v31sk806jr4a176i2jn6jgr9` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workflow_epochs` (
  `id` bigint NOT NULL,
  `active` bit(1) NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_30fctnhu807vuo452oabcd8y9` (`name`,`project_id`),
  KEY `FK_iib6u5p9cmplve6rb7y4fdicf` (`project_id`),
  CONSTRAINT `FK_iib6u5p9cmplve6rb7y4fdicf` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workflow_epochs_workflow_bins` (
  `workflow_epochs_id` bigint NOT NULL,
  `workflowBins_id` bigint NOT NULL,
  UNIQUE KEY `UK_kblymy4pbai6dyfbxft2v1mkw` (`workflowBins_id`),
  KEY `FK_d92ew9fy112rq2mtyg39p17c3` (`workflow_epochs_id`),
  CONSTRAINT `FK_d92ew9fy112rq2mtyg39p17c3` FOREIGN KEY (`workflow_epochs_id`) REFERENCES `workflow_epochs` (`id`),
  CONSTRAINT `FK_kblymy4pbai6dyfbxft2v1mkw` FOREIGN KEY (`workflowBins_id`) REFERENCES `workflow_bins` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklist_authors` (
  `WorklistJpa_id` bigint NOT NULL,
  `authors` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_86d23xhmh05vuodxm7uvg6xkx` (`WorklistJpa_id`),
  CONSTRAINT `FK_86d23xhmh05vuodxm7uvg6xkx` FOREIGN KEY (`WorklistJpa_id`) REFERENCES `worklists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklist_notes` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `note` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `worklist_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_44tmxueus5xgvvcbc4o00mx3q` (`worklist_id`),
  CONSTRAINT `FK_44tmxueus5xgvvcbc4o00mx3q` FOREIGN KEY (`worklist_id`) REFERENCES `worklists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklist_reviewers` (
  `WorklistJpa_id` bigint NOT NULL,
  `reviewers` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  KEY `FK_kufo7sgfxws3uteowjv45p338` (`WorklistJpa_id`),
  CONSTRAINT `FK_kufo7sgfxws3uteowjv45p338` FOREIGN KEY (`WorklistJpa_id`) REFERENCES `worklists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklistjpa_workflowstatehistory` (
  `WorklistJpa_id` bigint NOT NULL,
  `workflowStateHistory` datetime DEFAULT NULL,
  `workflowStateHistory_KEY` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`WorklistJpa_id`,`workflowStateHistory_KEY`),
  CONSTRAINT `FK_pkki0nwmsbvtfmotgoncc9ihm` FOREIGN KEY (`WorklistJpa_id`) REFERENCES `worklists` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklists` (
  `id` bigint NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `timestamp` datetime NOT NULL,
  `description` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `authorTime` bigint DEFAULT NULL,
  `epoch` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `number` int NOT NULL,
  `reviewerTime` bigint DEFAULT NULL,
  `team` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `workflowBinName` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `workflowStatus` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_gg7vhkiopdnd0vfx6hi3iiurs` (`name`,`workflowBinName`,`project_id`),
  KEY `FK_qunwpykssgwur26a94dbnjl2v` (`project_id`),
  CONSTRAINT `FK_qunwpykssgwur26a94dbnjl2v` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklists_tracking_records` (
  `worklists_id` bigint NOT NULL,
  `trackingRecords_id` bigint NOT NULL,
  UNIQUE KEY `UK_eu8mor5fp2cy3ln4rox759hb4` (`trackingRecords_id`),
  KEY `FK_dcruk2s2lo98s6as3grtsq9m7` (`worklists_id`),
  CONSTRAINT `FK_dcruk2s2lo98s6as3grtsq9m7` FOREIGN KEY (`worklists_id`) REFERENCES `worklists` (`id`),
  CONSTRAINT `FK_eu8mor5fp2cy3ln4rox759hb4` FOREIGN KEY (`trackingRecords_id`) REFERENCES `tracking_records` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50001 DROP VIEW IF EXISTS `ambig_concepts`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb3 */;
/*!50001 SET character_set_results     = utf8mb3 */;
/*!50001 SET collation_connection      = utf8mb3_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50001 VIEW `ambig_concepts` AS select distinct `c1`.`id` AS `conceptId1`,`c2`.`id` AS `conceptId2` from (((((`concepts` `c1` join `concepts_atoms` `ca1`) join `atoms` `a1`) join `concepts` `c2`) join `concepts_atoms` `ca2`) join `atoms` `a2`) where ((`c1`.`terminology` = 'NCIMTH') and (`c2`.`terminology` = 'NCIMTH') and (`c1`.`id` = `ca1`.`concepts_id`) and (`ca1`.`atoms_id` = `a1`.`id`) and (`c2`.`id` = `ca2`.`concepts_id`) and (`ca2`.`atoms_id` = `a2`.`id`) and (`c1`.`id` < `c2`.`id`) and (`a1`.`lowerNameHash` = `a2`.`lowerNameHash`) and (`a1`.`id` <> `a2`.`id`) and (`a1`.`publishable` = 1) and (`a2`.`publishable` = 1) and (`a1`.`obsolete` = 0) and (`a2`.`obsolete` = 0)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `auis_m4`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb3 */;
/*!50001 SET character_set_results     = utf8mb3 */;
/*!50001 SET collation_connection      = utf8mb3_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50001 VIEW `auis_m4` AS select `a`.`id` AS `atom_id`,`b`.`alternateTerminologyIds` AS `aui` from (`atoms` `a` join `atomjpa_alternateterminologyids` `b`) where ((`a`.`id` = `b`.`AtomJpa_id`) and (`b`.`alternateTerminologyIds_KEY` = 'NCIMTH')) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `classes_m4`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb3 */;
/*!50001 SET character_set_results     = utf8mb3 */;
/*!50001 SET collation_connection      = utf8mb3_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50001 VIEW `classes_m4` AS select 1 AS `atom_d`,1 AS `name`,1 AS `terminology`,1 AS `version`,1 AS `publishable`,1 AS `sui`,1 AS `lui`,1 AS `code`,1 AS `scui`,1 AS `sdui`,1 AS `concept_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `ruis_m4`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb3 */;
/*!50001 SET character_set_results     = utf8mb3 */;
/*!50001 SET collation_connection      = utf8mb3_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50001 VIEW `ruis_m4` AS select 1 AS `relationship_id`,1 AS `type`,1 AS `rui` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
