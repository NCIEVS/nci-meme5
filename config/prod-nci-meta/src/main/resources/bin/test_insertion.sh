#!/bin/bash
#
# Script to prepare meme-test (and/or meme-release) for a test insertion.  Script prepares a manual snapshot of meme-edit, recreates meme-test (and/or meme-release) db from this meme-edit snapshot, copies the meme-edit lucene indexes to s3.
# Notes:
# target db should not exist when this script is run
# script takes about one hour to run and needs to be run after editing hours since it takes down tomcat
#


echo "------------------------------------------------------"
echo "Starting ...`/bin/date`"
echo "------------------------------------------------------"

usage='test_insertion.csh {meme-test|meme-release|both} '
TEST_DB='meme-test'
RELEASE_DB='meme-release'
both=0
awspath='/usr/local/bin'


if [ "$#" -eq 1 ]; then
    DB_NAME=$1
    SNAPSHOT_DATE=`/bin/date  +"%Y%m%d"`
else
    echo "ERROR: Wrong number of parameters"
    echo "usage: $usage"
    exit 1
fi

echo "DB_NAME:    $DB_NAME"
echo "SNAPSHOT_DATE:    $SNAPSHOT_DATE"

# if parameter indicates 'both' dbs to be created, start with the meme-test db
if [[ $DB_NAME = "both" ]]; then 
	DB_NAME=$TEST_DB
	both=1
        echo "Both meme-test and meme-release will be prepared";
fi

if [[ $DB_NAME -ne $DEV_DB && $DB_NAME -ne $TEST_DB && $DB_NAME -ne $RELEASE_DB ]]; then 
        echo "ERROR: DB must be $DEV_DB or $TEST_DB or $RELEASE_DB";
    exit 1
fi

cd /local/content/MEME/MEME5/ncim/bin

# stop application
sudo service tomcat-evs-meme stop

# remove meme-edit-manual-snapshot if exists
echo "starting deletion of old meme-edit-manual-snapshot"
$awspath/aws rds delete-db-snapshot --profile meme --db-snapshot-identifier meme-edit-manual-snapshot


while :
do

   echo "deleting"
   started=`$awspath/aws rds describe-db-snapshots --profile meme --query "DBSnapshots[?DBSnapshotIdentifier=='meme-edit-manual-snapshot'].[Status][0][0]"`
   echo $started

      if [[ -n $started || $started == 'null' ]]; then
	echo "deleted"
 	break
      fi

      sleep 1
done



# create new meme-edit-manual-snapshot
echo "starting creation of new meme-edit-manual-snapshot"
$awspath/aws rds create-db-snapshot --profile meme --db-instance-identifier meme-edit --db-snapshot-identifier meme-edit-manual-snapshot

started='creating snapshot'
   while :
   do

   started=`$awspath/aws rds describe-db-snapshots --profile meme --query "DBSnapshots[?DBSnapshotIdentifier=='meme-edit-manual-snapshot'].[Status][0][0]" `
   echo $started
         if [[ $started =~ 'available' ]]; then
              echo ""
              break
           fi

           sleep 1
done



# recreate meme-test/release db from meme-edit-manual-snapshot
echo "starting meme-test/release db recreation"
$awspath/aws rds restore-db-instance-from-db-snapshot --profile meme --db-instance-identifier $DB_NAME --db-snapshot-identifier meme-edit-manual-snapshot --db-parameter-group-name meme-db --availability-zone us-east-1d --db-subnet-group-name default-vpc-dca724a4 --vpc-security-group-ids sg-05993d12d18c40cae

started='creating db'
   while :
   do

   started=`$awspath/aws rds describe-db-instances --profile meme --query "DBInstances[?DBInstanceIdentifier=='$DB_NAME'].[DBInstanceStatus][0][0]" `
   echo $started
         if [[ $started =~ 'available' ]]; then
              echo ""
              break
           fi
           sleep 1
done


# if 'both' parameter was indicated, meme-release will also be prepared/created from the meme-edit snapshot
if [[ $both -eq 1 ]]; then 
	DB_NAME=$RELEASE_DB
	echo "Both was indicated. meme-release creation will now be processed. "


  $awspath/aws rds restore-db-instance-from-db-snapshot --profile meme --db-instance-identifier $DB_NAME --db-snapshot-identifier meme-edit-manual-snapshot --db-parameter-group-name meme-db --availability-zone us-east-1d --db-subnet-group-name default-vpc-dca724a4 --vpc-security-group-ids sg-05993d12d18c40cae

  started='creating db'
     while :
     do

     started=`$awspath/aws rds describe-db-instances --profile meme --query "DBInstances[?DBInstanceIdentifier=='$DB_NAME'].[DBInstanceStatus][0][0]" `
     echo $started
         if [[ $started =~ 'available' ]]; then
              echo ""
              break
           fi
           sleep 1
  done
fi

# make copy of meme-edit's lucene indexes and store in s3
echo "meme-test/release db recreate COMPLETED"
echo "starting backupIndexes.csh"

./backupIndexes.csh manual

# restart application
sudo service tomcat-evs-meme start


echo "------------------------------------------------------"
echo "Finished ...`/bin/date`"
echo "------------------------------------------------------"


# NEXT STEPS on meme-test (and/or meme-release if clone is to meme-release)
# 1. pull indexes
#   tstop
#   cd /local/content/MEME/MEME5/ncim/data/indexes
#   rm -rf *
#   aws s3 cp s3://nci-evs-meme/indexes/manual_20231018.tar .
#   tar -xvf manual_20231018.tar
#   rm manual_20231018.tar
#   chmod -R 777 /local/content/MEME/MEME5/ncim/data/indexes/
#   chown -R tomcata:tomcata /local/content/MEME/MEME5/ncim/data/indexes/ 
# 2. cp insertion data from s3
#    cd /local/content/MEME/MEME5/ncim/bin
#    ./pull_s3.csh inv NCI_2023_09D
#    ./prep_insertion.csh -t NCI_2023_09D
# 3. start tomcat and test application functioning
#   tstart
#   http://ncias-q3009-c.nci.nih.gov:8080/ncim-server-rest/index.html#/login
# 4. set up insertion recipe

