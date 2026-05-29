# global service name
SERVICE                 := nci-meme5

#######################################################################
#                 OVERRIDE THIS TO MATCH YOUR PROJECT                 #
#######################################################################
APP_VERSION             := $(shell grep "^version =" build.gradle | sed "s/version = //; s/'//g")
VERSION                 := $(shell grep "^version =" build.gradle | sed "s/version = //; s/'//g; s/-SNAPSHOT//")

# Builds should be repeatable, therefore we need a method to reference the git
# sha where a version came from.
GIT_VERSION             ?= $(shell git describe --match=NeVeRmAtCh --always --dirty)
GIT_COMMIT              ?= $(shell git log -1 --format=%H)
GIT_COMMITTED_AT        ?= $(shell git log -1 --format=%ct)
GIT_BRANCH              ?=
FULL_VERSION            := v$(APP_VERSION)-g$(GIT_VERSION)

SHELL                   := /bin/bash
GRADLEW                 ?= ./gradlew
ENV_FILE                ?= config/local/setenv.sh
WITH_ENV                := source $(ENV_FILE)
WITHOUT_LOCAL_PATH_ENV  := unset APP_DIR CATALINA_BASE DATA_DIR INDEX_DIR LVG_DIR SOURCE_DATA_DIR
UNIT_TEST_PATTERN       ?= *UnitTest
FLYWAY_IT_JDBC_URL      ?=
FLYWAY_IT_BASELINE_JDBC_URL ?=
FLYWAY_IT_USER          ?= root
FLYWAY_IT_PASSWORD      ?=
FLYWAY_IT_HOST          ?= 127.0.0.1
FLYWAY_IT_PORT          ?= 3306
FLYWAY_IT_SCHEMA_PREFIX ?= ncimdb_nm306
FLYWAY_IT_RUN_ID        := $(if $(FLYWAY_IT_RUN_ID),$(FLYWAY_IT_RUN_ID),$(shell date +%Y%m%d%H%M%S))
FLYWAY_IT_EPHEMERAL_JDBC_URL := jdbc:mysql://$(FLYWAY_IT_HOST):$(FLYWAY_IT_PORT)/$(FLYWAY_IT_SCHEMA_PREFIX)_flyway_it_$(FLYWAY_IT_RUN_ID)
FLYWAY_IT_EPHEMERAL_BASELINE_JDBC_URL := jdbc:mysql://$(FLYWAY_IT_HOST):$(FLYWAY_IT_PORT)/$(FLYWAY_IT_SCHEMA_PREFIX)_flyway_base_it_$(FLYWAY_IT_RUN_ID)

.PHONY: help clean build test run quality preflight-sample preflight-ncimeta \
	preflight-rest preflight-insertion preflight-admin preflight-flyway \
	prepare-sample prepare-ncimeta prepare-insertion prepare-admin \
	prepare-flyway \
	integration-sample integration-ncimeta integration-rest \
	integration-insertion integration-admin integration-flyway \
	integration-flyway-ephemeral \
	migrate migrate-info migrate-validate scan version

help:
	@echo "Common targets for $(SERVICE):"
	@echo "  make build            Clean and assemble Gradle artifacts"
	@echo "  make test             Run the unit test suite"
	@echo "  make run              Start the app locally with Spring Boot"
	@echo "  make quality          Run Gradle verification checks and unit tests"
	@echo "  make preflight-sample Check sample JPA integration prerequisites"
	@echo "  make preflight-ncimeta Check NCI-META JPA integration prerequisites"
	@echo "  make preflight-rest   Check REST integration prerequisites"
	@echo "  make preflight-insertion Check insertion integration prerequisites"
	@echo "  make preflight-admin  Check admin/load integration prerequisites"
	@echo "  make preflight-flyway Check Flyway smoke-test prerequisites"
	@echo "  make prepare-sample Create/load/reindex sample integration DB fixture"
	@echo "  make prepare-ncimeta Create/load/generate/reindex NCI-META DB fixture"
	@echo "  make prepare-insertion Create/load/generate/reindex insertion DB fixture"
	@echo "  make prepare-admin Create empty disposable admin/load DB fixture"
	@echo "  make prepare-flyway Create missing empty Flyway smoke-test schemas"
	@echo "  make integration-sample Run sample JPA integration tests"
	@echo "  make integration-ncimeta Run NCI-META JPA integration tests"
	@echo "  make integration-rest Run REST integration tests"
	@echo "  make integration-insertion Run insertion integration tests"
	@echo "  make integration-admin Run admin/load integration tests"
	@echo "  make integration-flyway Run Flyway smoke integration tests"
	@echo "  make integration-flyway-ephemeral Run Flyway smoke tests with generated schemas"
	@echo "  make migrate          Run Flyway migrations"
	@echo "  make migrate-info     Show Flyway migration status"
	@echo "  make migrate-validate Validate Flyway migrations"
	@echo "  make scan             Run strict Trivy dependency vulnerability scan"

clean:
	$(WITH_ENV); $(GRADLEW) clean

build:
	$(WITH_ENV); $(GRADLEW) clean assemble

test:
	$(WITH_ENV); $(WITHOUT_LOCAL_PATH_ENV); $(GRADLEW) test --tests '$(UNIT_TEST_PATTERN)'

run:
	$(WITH_ENV); $(GRADLEW) bootRun

quality:
	$(WITH_ENV); $(WITHOUT_LOCAL_PATH_ENV); $(GRADLEW) check -x test
	$(WITH_ENV); $(WITHOUT_LOCAL_PATH_ENV); $(GRADLEW) test --tests '$(UNIT_TEST_PATTERN)'

preflight-sample:
	$(WITH_ENV); $(GRADLEW) sampleJpaIntegrationPreflight

preflight-ncimeta:
	$(WITH_ENV); $(GRADLEW) nciMetaJpaIntegrationPreflight

preflight-rest:
	$(WITH_ENV); $(GRADLEW) restIntegrationPreflight

preflight-insertion:
	$(WITH_ENV); $(GRADLEW) insertionIntegrationPreflight

preflight-admin:
	$(WITH_ENV); $(GRADLEW) adminLoaderIntegrationPreflight

preflight-flyway:
	$(WITH_ENV); $(GRADLEW) flywayIntegrationPreflight \
		-Dflyway.it.enabled=true \
		-Dflyway.it.jdbcUrl='$(FLYWAY_IT_JDBC_URL)' \
		-Dflyway.it.baselineJdbcUrl='$(FLYWAY_IT_BASELINE_JDBC_URL)' \
		-Dflyway.it.user='$(FLYWAY_IT_USER)' \
		-Dflyway.it.password='$(FLYWAY_IT_PASSWORD)'

prepare-sample:
	$(WITH_ENV); $(GRADLEW) prepareSampleIntegrationData

prepare-ncimeta:
	$(WITH_ENV); $(GRADLEW) prepareNciMetaIntegrationData

prepare-insertion:
	$(WITH_ENV); $(GRADLEW) prepareInsertionIntegrationData

prepare-admin:
	$(WITH_ENV); $(GRADLEW) prepareAdminLoaderIntegrationData

prepare-flyway:
	$(WITH_ENV); $(GRADLEW) prepareFlywayIntegrationSchemas \
		-Dflyway.it.enabled=true \
		-Dflyway.it.jdbcUrl='$(FLYWAY_IT_JDBC_URL)' \
		-Dflyway.it.baselineJdbcUrl='$(FLYWAY_IT_BASELINE_JDBC_URL)' \
		-Dflyway.it.user='$(FLYWAY_IT_USER)' \
		-Dflyway.it.password='$(FLYWAY_IT_PASSWORD)'

integration-sample:
	$(WITH_ENV); $(GRADLEW) sampleJpaIntegrationTest

integration-ncimeta:
	$(WITH_ENV); $(GRADLEW) nciMetaJpaIntegrationTest

integration-rest:
	$(WITH_ENV); $(GRADLEW) restIntegrationTest

integration-insertion:
	$(WITH_ENV); $(GRADLEW) insertionIntegrationTest

integration-admin:
	$(WITH_ENV); $(GRADLEW) adminLoaderIntegrationTest

integration-flyway:
	$(WITH_ENV); $(GRADLEW) flywayIntegrationTest \
		-Dflyway.it.enabled=true \
		-Dflyway.it.jdbcUrl='$(FLYWAY_IT_JDBC_URL)' \
		-Dflyway.it.baselineJdbcUrl='$(FLYWAY_IT_BASELINE_JDBC_URL)' \
		-Dflyway.it.user='$(FLYWAY_IT_USER)' \
		-Dflyway.it.password='$(FLYWAY_IT_PASSWORD)'

integration-flyway-ephemeral:
	$(WITH_ENV); $(GRADLEW) ephemeralFlywayIntegrationTest \
		-Dflyway.it.enabled=true \
		-Dflyway.it.ephemeral=true \
		-Dflyway.it.runId='$(FLYWAY_IT_RUN_ID)' \
		-Dflyway.it.jdbcUrl='$(FLYWAY_IT_EPHEMERAL_JDBC_URL)' \
		-Dflyway.it.baselineJdbcUrl='$(FLYWAY_IT_EPHEMERAL_BASELINE_JDBC_URL)' \
		-Dflyway.it.user='$(FLYWAY_IT_USER)' \
		-Dflyway.it.password='$(FLYWAY_IT_PASSWORD)'

migrate:
	$(WITH_ENV); $(GRADLEW) adminFlywayMigrate

migrate-info:
	$(WITH_ENV); $(GRADLEW) adminFlywayInfo

migrate-validate:
	$(WITH_ENV); $(GRADLEW) adminFlywayValidate

scan:
	set -e; $(WITH_ENV); $(WITHOUT_LOCAL_PATH_ENV); \
	cleanup() { /bin/rm -rf gradle/dependency-locks gradle.lockfile; }; \
	trap cleanup EXIT; \
	$(GRADLEW) dependencies --write-locks; \
	trivy fs gradle.lockfile --scanners vuln --severity HIGH,CRITICAL --exit-code 1 \
		--format template -o report.html --template "@config/trivy/html.tpl"

version:
	@echo $(APP_VERSION)
