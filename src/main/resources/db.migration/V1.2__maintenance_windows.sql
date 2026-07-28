--
-- Stores planned maintenance windows used to warn process operators.
--

CREATE TABLE `maintenance_windows` (
  `id` bigint NOT NULL,
  `endDate` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  `lastModifiedBy` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `startDate` datetime NOT NULL,
  `timestamp` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_maintenance_windows_start_date` (`startDate`),
  KEY `idx_maintenance_windows_end_date` (`endDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
