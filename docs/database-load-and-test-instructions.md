# Database Load And Test Instructions

## Build The Code

```sh
./gradlew explodeWar -Prun.config.label=umls
```

## Load The Sample DB

```sh
./gradlew adminCreateDb -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.sample.properties

./gradlew adminLoadRrfUmls -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.sample.properties -Pterminology=MTH -Pinput.dir=config/src/main/resources/data/SAMPLE_UMLS

./gradlew adminReindex -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.sample.properties

./gradlew adminGenerateSampleData -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.sample.properties -Pterminology=MTH
```

## Sample Tests

Config: `config.sample.properties`

```sh
./gradlew integrationTest -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.sample.properties --tests 'com.wci.umls.server.test.jpa.integrity.*' --tests 'com.wci.umls.server.test.jpa.search.*' --tests com.wci.umls.server.test.jpa.AddDemotionIT --tests com.wci.umls.server.test.jpa.ComponentStatsIT --tests com.wci.umls.server.test.jpa.ContentDeepRelsIT --tests com.wci.umls.server.test.jpa.ContentServiceAutocompleteIT --tests com.wci.umls.server.test.jpa.ContentServiceFindRelationshipsIT --tests com.wci.umls.server.test.jpa.ProjectJpaIT --tests com.wci.umls.server.test.jpa.UpdateConceptStatusIT --tests com.wci.umls.server.test.jpa.algorithm.FailOnceAlgorithmIT --tests com.wci.umls.server.test.jpa.algorithm.MatrixInitializerAlgorithmIT --tests com.wci.umls.server.test.jpa.algorithm.QueryActionAlgorithmIT --tests com.wci.umls.server.test.jpa.algorithm.SemanticTypeResolverAlgorithmIT --tests com.wci.umls.server.test.jpa.algorithm.WaitAlgorithmIT -x javadoc
```

### Sample Tests That Require Tomcat Running

```sh
./gradlew integrationTest -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.sample.properties --tests 'com.wci.umls.server.test.rest.SecurityServiceRestDegenerateUseIT' --tests 'com.wci.umls.server.test.rest.SecurityServiceRestEdgeCasesIT' --tests 'com.wci.umls.server.test.rest.SecurityServiceRestIT' --tests 'com.wci.umls.server.test.rest.SecurityServiceRestNormalUseIT' --tests 'com.wci.umls.server.test.rest.SecurityServiceRestRoleCheckIT' --tests 'com.wci.umls.server.test.rest.WorkflowServiceRestNormalUseIT' --tests 'com.wci.umls.server.test.rest.meta.*'
```

## Load The NCI-META Database

```sh
./gradlew adminCreateDb -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.ncimeta.properties

./gradlew adminLoadRrfUmls -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.ncimeta.properties -Pterminology=NCIMTH -Pinput.dir=config/src/main/resources/data/SAMPLE_NCI -Pedit.mode=true -Pserver=false -Pmode=create -Pversion=latest -Pprefix=MR

./gradlew adminGenerateNciMetaData -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.ncimeta.properties -Pterminology=NCIMTH -Pversion=latest -Pmode=update -Pinput.dir=/Users/deborahshapiro/Code/workspace-meme/nci-meme5/config/src/main/resources/data/SAMPLE_NCI

./gradlew adminReindex -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.ncimeta.properties
```

## NCI-META Tests

Config: `config.ncimeta.properties`

```sh
./gradlew integrationTest -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.ncimeta.properties --tests com.wci.umls.server.test.jpa.CloseReopenFactoryIT --tests com.wci.umls.server.test.jpa.ComputePreferredNameHandlerIT --tests com.wci.umls.server.test.jpa.ConfigUtilityIT --tests com.wci.umls.server.test.jpa.ContentServiceGeneralQueryTimeoutIT --tests com.wci.umls.server.test.jpa.ContentServiceTreePositionFromTreeIT --tests com.wci.umls.server.test.jpa.DefaultValidationCheckIT --tests com.wci.umls.server.test.jpa.GraphResolutionHandlerIT --tests com.wci.umls.server.test.jpa.IdentifierAssignmentHandlerIT --tests com.wci.umls.server.test.jpa.MappingIT --tests com.wci.umls.server.test.jpa.ProgressEventIT --tests com.wci.umls.server.test.jpa.ReportHelperIT --tests com.wci.umls.server.test.jpa.SemanticCategorySearchIT --tests com.wci.umls.server.test.jpa.UmlsIdentifierAssignmentHandlerIT --tests com.wci.umls.server.test.jpa.algorithm.AddRemoveIntegrityCheckAlgorithmIT --tests com.wci.umls.server.test.jpa.algorithm.ComponentInfoRelRemapperAlgorithmIT --tests com.wci.umls.server.test.jpa.algorithm.LexicalClassAssignmentAlgorithmIT --tests com.wci.umls.server.test.jpa.algorithm.MapSetLoaderAlgorithmIT --tests com.wci.umls.server.test.jpa.algorithm.SubsetLoaderAlgorithmIT --tests com.wci.umls.server.test.algo.ComputePreferredNamesAlgorithmIT --tests com.wci.umls.server.test.helpers.PfsParameterForComponentIT --tests com.wci.umls.server.test.helpers.PfsParameterForConceptIT --tests com.wci.umls.server.test.helpers.SearchHandlerIT
```

### NCI-META Tests That Require Tomcat Running

```sh
./gradlew integrationTest -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.ncimeta.properties --tests 'com.wci.umls.server.test.rest.ncimeta.*'
```

## Insert Tests

Run the NCI-META load steps with `config.insert.properties`, then run the following.

FYI: `InsertionLoaderAlgorithmsIT` calls `MetadataLoaderAlgorithmIT`, `AtomLoaderAlgorithmIT`, `RelationshipLoaderAlgorithmIT`, `ContextLoaderAlgorithmIT`, `SemanticTypeLoaderAlgorithmIT`, and `AttributeLoaderAlgorithmIT`.

```sh
./gradlew integrationTest -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.insert.properties --tests "com.wci.umls.server.test.jpa.algorithm.InsertionLoaderAlgorithmsIT"

./gradlew integrationTest -Drun.config.umls=/Users/deborahshapiro/Code/workspace-meme/meme-jdk17/config/config.insert.properties --tests "com.wci.umls.server.test.jpa.algorithm.PreInsertionAlgorithmIT" --tests "com.wci.umls.server.test.jpa.algorithm.BequeathAlgorithmIT" --tests "com.wci.umls.server.test.jpa.algorithm.GeneratedMergeAlgorithmIT" --tests "com.wci.umls.server.test.jpa.algorithm.PrecomputedMergeAlgorithmIT" --tests "com.wci.umls.server.test.jpa.algorithm.UpdatePublishedAlgorithmIT" --tests "com.wci.umls.server.test.jpa.algorithm.ProdMidCleanupAlgorithmIT" --tests "com.wci.umls.server.test.jpa.algorithm.UpdateReleasibilityAlgorithmIT" --tests "com.wci.umls.server.test.jpa.algorithm.SafeReplaceAlgorithmIT"
```
