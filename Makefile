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

.PHONY: help clean build test run quality migrate migrate-info migrate-validate scan version

help:
	@echo "Common targets for $(SERVICE):"
	@echo "  make build            Clean and assemble Gradle artifacts"
	@echo "  make test             Run the unit test suite"
	@echo "  make run              Start the app locally with Spring Boot"
	@echo "  make quality          Run Gradle verification checks and unit tests"
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
