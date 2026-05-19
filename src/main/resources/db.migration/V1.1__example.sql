--
-- Low-risk Flyway verification migration. This creates and immediately drops
-- a temporary object so Flyway history records a post-baseline migration
-- without changing application behavior.
--

CREATE TABLE flyway_migration_smoke_test (
  id BIGINT NOT NULL,
  note VARCHAR(64) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

DROP TABLE flyway_migration_smoke_test;
