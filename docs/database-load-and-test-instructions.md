# Database Load And Test Instructions

## NM-278 Configuration Model

The normal path is now Spring-style application properties plus environment
variables:

- `src/main/resources/application.properties`
- `src/main/resources/application-*.properties`
- `config/local/setenv.sh`

Legacy `-Drun.config.*=/path/to/config.properties` files no longer override
the Spring-backed configuration. A gated migration fallback still exists, but
only for older runtimes where the Spring application property bridge is
unavailable.

For each flow below, set any local overrides first, then source the bootstrap:

```sh
export DB_NAME=<target-db>
source config/local/setenv.sh
```

Use a different `DB_NAME`, `APP_DIR`, `DATA_DIR`, `INDEX_DIR`, or
`SOURCE_DATA_DIR` before sourcing the script when a flow needs a separate
database or filesystem layout. If `DATA_DIR` is changed, set `INDEX_DIR` too.
Set `APP_DIR` before deriving any other paths from it.

## Build The Code

```sh
source config/local/setenv.sh
./gradlew explodeWar
```

## Load The Sample DB

Run these with Tomcat stopped.

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbmeta
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

./gradlew adminCreateDb

./gradlew adminLoadRrfUmls \
  -Pterminology=MTH \
  -Pinput.dir=config/src/main/resources/data/SAMPLE_UMLS \
  -Pprefix=MR \
  -Pedit.mode=false \
  -Pserver=false

./gradlew adminReindex -Pserver=false

./gradlew adminGenerateSampleData \
  -Pterminology=MTH
```

## Sample Tests

Run JPA tests with Tomcat stopped:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbmeta
export DATA_DIR="$APP_DIR/data_sample"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

./gradlew integrationTest \
  --tests 'com.wci.umls.server.test.jpa.integrity.*' \
  --tests 'com.wci.umls.server.test.jpa.search.*' \
  --tests com.wci.umls.server.test.jpa.AddDemotionIT \
  --tests com.wci.umls.server.test.jpa.ComponentStatsIT \
  --tests com.wci.umls.server.test.jpa.ContentDeepRelsIT \
  --tests com.wci.umls.server.test.jpa.ContentServiceAutocompleteIT \
  --tests com.wci.umls.server.test.jpa.ContentServiceFindRelationshipsIT \
  --tests com.wci.umls.server.test.jpa.ProjectJpaIT \
  --tests com.wci.umls.server.test.jpa.UpdateConceptStatusIT \
  --tests com.wci.umls.server.test.jpa.algorithm.FailOnceAlgorithmIT \
  --tests com.wci.umls.server.test.jpa.algorithm.MatrixInitializerAlgorithmIT \
  --tests com.wci.umls.server.test.jpa.algorithm.QueryActionAlgorithmIT \
  --tests com.wci.umls.server.test.jpa.algorithm.SemanticTypeResolverAlgorithmIT \
  --tests com.wci.umls.server.test.jpa.algorithm.WaitAlgorithmIT \
  -x javadoc
```

Run REST tests with Tomcat running from the same sourced environment:

```sh
./gradlew integrationTest \
  --tests 'com.wci.umls.server.test.rest.SecurityServiceRestDegenerateUseIT' \
  --tests 'com.wci.umls.server.test.rest.SecurityServiceRestEdgeCasesIT' \
  --tests 'com.wci.umls.server.test.rest.SecurityServiceRestIT' \
  --tests 'com.wci.umls.server.test.rest.SecurityServiceRestNormalUseIT' \
  --tests 'com.wci.umls.server.test.rest.SecurityServiceRestRoleCheckIT' \
  --tests 'com.wci.umls.server.test.rest.WorkflowServiceRestNormalUseIT' \
  --tests 'com.wci.umls.server.test.rest.meta.*'
```

## Load The NCI-META Database

Run these with Tomcat stopped.

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbncimeta
export DATA_DIR="$APP_DIR/data_ncimeta"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

./gradlew adminCreateDb

./gradlew adminLoadRrfUmls \
  -Pterminology=NCIMTH \
  -Pversion=latest \
  -Pprefix=MR \
  -Pinput.dir=config/src/main/resources/data/SAMPLE_NCI \
  -Pedit.mode=true \
  -Pserver=false \
  -Pmode=create

./gradlew adminGenerateNciMetaData \
  -Pterminology=NCIMTH \
  -Pversion=latest \
  -Pmode=update \
  -Pinput.dir=config/src/main/resources/data/SAMPLE_NCI

./gradlew adminReindex -Pserver=false
```

## NCI-META Tests

Run JPA tests with Tomcat stopped:

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbncimeta
export DATA_DIR="$APP_DIR/data_ncimeta"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$APP_DIR/data"
source config/local/setenv.sh

./gradlew integrationTest \
  --tests com.wci.umls.server.test.jpa.CloseReopenFactoryIT \
  --tests com.wci.umls.server.test.jpa.ComputePreferredNameHandlerIT \
  --tests com.wci.umls.server.test.jpa.ConfigUtilityIT \
  --tests com.wci.umls.server.test.jpa.ContentServiceGeneralQueryTimeoutIT \
  --tests com.wci.umls.server.test.jpa.ContentServiceTreePositionFromTreeIT \
  --tests com.wci.umls.server.test.jpa.DefaultValidationCheckIT \
  --tests com.wci.umls.server.test.jpa.GraphResolutionHandlerIT \
  --tests com.wci.umls.server.test.jpa.IdentifierAssignmentHandlerIT \
  --tests com.wci.umls.server.test.jpa.MappingIT \
  --tests com.wci.umls.server.test.jpa.ProgressEventIT \
  --tests com.wci.umls.server.test.jpa.ReportHelperIT \
  --tests com.wci.umls.server.test.jpa.SemanticCategorySearchIT \
  --tests com.wci.umls.server.test.jpa.UmlsIdentifierAssignmentHandlerIT \
  --tests com.wci.umls.server.test.jpa.algorithm.AddRemoveIntegrityCheckAlgorithmIT \
  --tests com.wci.umls.server.test.jpa.algorithm.ComponentInfoRelRemapperAlgorithmIT \
  --tests com.wci.umls.server.test.jpa.algorithm.LexicalClassAssignmentAlgorithmIT \
  --tests com.wci.umls.server.test.jpa.algorithm.MapSetLoaderAlgorithmIT \
  --tests com.wci.umls.server.test.jpa.algorithm.SubsetLoaderAlgorithmIT \
  --tests com.wci.umls.server.test.algo.ComputePreferredNamesAlgorithmIT \
  --tests com.wci.umls.server.test.helpers.PfsParameterForComponentIT \
  --tests com.wci.umls.server.test.helpers.PfsParameterForConceptIT \
  --tests com.wci.umls.server.test.helpers.SearchHandlerIT
```

Run a REST smoke test with Tomcat running from the same sourced environment:

```sh
./gradlew integrationTest \
  --tests com.wci.umls.server.test.rest.SecurityServiceRestNormalUseIT
```

The `com.wci.umls.server.test.rest.ncimeta.*` meta-editing classes are
currently annotated with JUnit `@Ignore`, so this command selects them but
reports them as skipped unless those classes are deliberately re-enabled:

```sh
./gradlew integrationTest \
  --tests 'com.wci.umls.server.test.rest.ncimeta.*'
```

## Insert Tests

Use the same env-backed model as the NCI-META flow, but choose the insertion
database and data directories that match the insertion test data.

```sh
export APP_DIR="$(cd .. && pwd)/meme-jdk17"
export DB_NAME=ncimdbinsert
export DATA_DIR="$APP_DIR/data_insert"
export INDEX_DIR="$DATA_DIR/indexes-jdk17"
export SOURCE_DATA_DIR="$DATA_DIR"
source config/local/setenv.sh
```

Run the NCI-META load steps against that selected insertion database, then run:

```sh
./gradlew integrationTest \
  --tests "com.wci.umls.server.test.jpa.algorithm.InsertionLoaderAlgorithmsIT"

./gradlew integrationTest \
  --tests "com.wci.umls.server.test.jpa.algorithm.PreInsertionAlgorithmIT" \
  --tests "com.wci.umls.server.test.jpa.algorithm.BequeathAlgorithmIT" \
  --tests "com.wci.umls.server.test.jpa.algorithm.GeneratedMergeAlgorithmIT" \
  --tests "com.wci.umls.server.test.jpa.algorithm.PrecomputedMergeAlgorithmIT" \
  --tests "com.wci.umls.server.test.jpa.algorithm.UpdatePublishedAlgorithmIT" \
  --tests "com.wci.umls.server.test.jpa.algorithm.ProdMidCleanupAlgorithmIT" \
  --tests "com.wci.umls.server.test.jpa.algorithm.UpdateReleasibilityAlgorithmIT" \
  --tests "com.wci.umls.server.test.jpa.algorithm.SafeReplaceAlgorithmIT"
```

`InsertionLoaderAlgorithmsIT` calls `MetadataLoaderAlgorithmIT`,
`AtomLoaderAlgorithmIT`, `RelationshipLoaderAlgorithmIT`,
`ContextLoaderAlgorithmIT`, `SemanticTypeLoaderAlgorithmIT`, and
`AttributeLoaderAlgorithmIT`.

## Legacy Fallback

The legacy form is retained only as migration documentation. In the current
runtime, `application*.properties` remain primary even if `run.config.*` is
passed.

```sh
./gradlew adminCreateDb \
  -Dconfig.legacy.runConfig.enabled=true \
  -Drun.config.umls=/path/to/config.properties
```
