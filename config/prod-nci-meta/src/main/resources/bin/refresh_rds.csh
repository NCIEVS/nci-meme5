#!/bin/tcsh -f
#
# This script is used to create a new rds db from the latest backup of meme-edit.  If you don't want the latest automatic backup or if you want a backup other than the meme-edit db, do the refresh manually from the UI.  
#

# Configuration
set AWS_BIN = "aws"
set AWS_PROFILE = "meme"
setenv TEST_DB meme-test
setenv DEV_DB meme-dev
setenv RELEASE_DB meme-release
set MEME_SOURCE_DB = "meme-edit"
set RDS_PARAMETER_GROUP = "meme-db"
set RDS_AVAILABILITY_ZONE = "us-east-1d"
set RDS_SUBNET_GROUP = "mysql-subnet-group"
set RDS_SECURITY_GROUP_IDS = "sg-05993d12d18c40cae"
set RDS_DEV_SECURITY_GROUP_IDS = "sg-0a42ddabf8c260525"
if ("$AWS_PROFILE" == "") then
  set aws = ( "$AWS_BIN" )
else
  set aws = ( "$AWS_BIN" --profile "$AWS_PROFILE" )
endif

setenv usage 'refresh_rds.csh {meme-test|meme-release|meme-dev} '

echo "--------------------------------------------------------"
echo "Starting `/bin/date`"
echo "--------------------------------------------------------"

if ($#argv == 1) then
    setenv DB_NAME $1
else
    echo "ERROR: Wrong number of parameters"
    echo "usage: $usage"
    exit 1
endif

echo "DB_NAME:    $DB_NAME"

if ($DB_NAME != $DEV_DB && $DB_NAME != $TEST_DB && $DB_NAME != $RELEASE_DB) then	
	echo "ERROR: DB created must be $DEV_DB or $TEST_DB or $RELEASE_DB" 
	exit 1
endif

if ($DB_NAME == "$DEV_DB") then
    setenv VPC "$RDS_DEV_SECURITY_GROUP_IDS"
else 
    setenv VPC "$RDS_SECURITY_GROUP_IDS"
endif

echo "VPC:    $VPC"


$aws rds restore-db-instance-to-point-in-time --source-db-instance-identifier "$MEME_SOURCE_DB" --target-db-instance-identifier $DB_NAME --use-latest-restorable-time --db-parameter-group-name "$RDS_PARAMETER_GROUP" --availability-zone "$RDS_AVAILABILITY_ZONE" --db-subnet-group-name "$RDS_SUBNET_GROUP" --vpc-security-group-ids $VPC --tags "Key"="autostart","Value"="true" "Key"="autostop","Value"="true"

echo ""

set started = null
while ($started != 'available')
   echo "refreshing"
   set started = `$aws rds describe-db-instances --query "DBInstances[?DBInstanceIdentifier=='$DB_NAME'].[DBInstanceStatus][0][0]" | jq -r`
end

echo "refresh completed"

echo "-----------------------------------------------------"
echo "Finished $0 ... `/bin/date`"
echo "-----------------------------------------------------"
