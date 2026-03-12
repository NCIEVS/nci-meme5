Test Data and Associated Integration Tests

meta  (-P rest-meta  for tests in integration-test/src/test/java/com/wci/umls/server/test/rest/meta/)
ResetDevDatabase  -> GenerateSampleData  terminology MTH latest
   first loads via RRF-umls
   SAMPLE_UMLS
     this indirectly loads these (among others)
       MTH latest
       SNOMEDCT_US 2016_03_01
       MSH 2016_2016_02_26

   then runs GenerateSampleData to add


ncimeta  (-P rest-ncimeta  for tests in integration-test/src/test/java/com/wci/umls/server/test/rest/ncimeta/)
ResetNciMetaDatabase  -> GenerateNciMetaDataMojo  terminology NCIMTH latest
uses processes to insert
   NCI 2016_11D
   SNOMEDCT_US 2016_09_01
   MTH 2016AB


demo (just for display purposes, not for integration tests)
ResetDemoDatabase  -> GenerateDemoData    adds projects for "SNOMEDCT", "SNOMEDCT_US", "ICD9CM", "ICD10CM", "LNC", "20160731", "20160901", "2013", "2016", "254"
   first loads via mojos
   SAMPLE_UMLS
   SNOMEDCT 20160731
   SNOMEDCT_US 20160901
   ICD-9-CM 2016
   ICD-10-CM 2015
   then runs GenerateDemoData to add users, processes, etc.


NOTE: for testing, there should be additional config.properties files, to ensure each test suite is pointing at the correct database
